package org.wordpress.android.poco.policy.staticAnalysis;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.poco.policy.CFG;
import org.wordpress.android.poco.policy.GenPolicyCFG;
import org.wordpress.android.poco.policy.Policy;
import org.wordpress.android.poco.policy.Utils;
import org.wordpress.android.poco.policy.runtime.RuntimeUtils;
import org.wordpress.android.poco.policy.staticAnalysis.scanPolicies.EventInfo;
import org.wordpress.android.poco.policy.staticAnalysis.scanPolicies.ParsFlgConsts;
import org.wordpress.android.poco.policy.staticAnalysis.scanPolicies.PolicyASTVisitor;
import org.wordpress.android.poco.policy.staticAnalysis.visitClasses.PolicyVisitor;

public class StaticAnalysis {
	private final static int api = Opcodes.ASM5;
	private static final String TAG = StaticAnalysis.class.getName();
	private ArrayList<String> _policyNames;
	private Map<String, Map<String, CFG>> _poicy2CFGs;
	private Map<String, Set<String>> _policy2ConcernedEvts = new HashMap<>();

	public StaticAnalysis(Policy[] policies) {
		// step 1: get declared policy names (e.g., edu.usfcse.poco.poco_demo.examplePolicies.P_confirm)
		_policyNames = getPolicyNames(policies);
		_poicy2CFGs = new HashMap<String, Map<String, CFG>>();
		// step 2: analyze policy source and class files
        analyzePolicies();
	}

    private void analyzePolicies() {
		String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/temp/";
		ZipFile zipfiles = null;
		JarFile jarfiles = null;
		try {
			zipfiles = getJarFiles(path, "temp.zip", R.raw.policies);
			jarfiles = getJarFiles(path, "temp.jar", R.raw.policy_clzfiles);
            scanPolicies(zipfiles, jarfiles);
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			closeZipFile(zipfiles);
			closeZipFile(jarfiles);
		}
	}

	private void closeZipFile(ZipFile zipFile) {
		if(zipFile != null) {
			try {
				zipFile.close();
			} catch (IOException e) {
				Log.e(TAG, "Failed to analyze PoCo policy files, please check!");
				System.exit(-1);
			}
		}
	}

	private void scanPolicies(ZipFile zipfiles, JarFile jarFiles) {
		Enumeration policyEntries = zipfiles.entries();
		while (policyEntries.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) policyEntries.nextElement();
			String fileName = entry.getName();
			if(!fileName.endsWith(".java"))
			    continue;
			String policyName= getName(fileName);
			if(fileName.endsWith(".java") && _policyNames.contains(policyName)) {
                LinkedHashMap<String, ArrayList<EventInfo>> mtd2EvtSigs = scanPolicy(zipfiles, entry);
                Map<String, ArrayList<String>> mtd2DetailedEvtSigs = analyzeClassFiles(policyName,jarFiles);
				mergeSigInfo(mtd2EvtSigs, mtd2DetailedEvtSigs);
				genCFGs(policyName, mtd2EvtSigs);
			}
		}
    }

    private  String getName (String pName) {
        final String reg = "^(.+\\/)(.+)\\.java$";
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(pName);
        return matcher.find() ? matcher.group(2) : pName;
    }

    private LinkedHashMap<String, ArrayList<EventInfo>> scanPolicy(ZipFile zipfiles, ZipEntry entry) {

		try(InputStream is4code = zipfiles.getInputStream(entry);
			InputStreamReader isr = new InputStreamReader(is4code)) {

			StringBuilder sb = new StringBuilder();
			try (Reader reader = new BufferedReader(new InputStreamReader(is4code, isr.getEncoding()))) {
				int c = 0;
				while ((c = reader.read()) != -1) { sb.append((char) c);}
			}
			String source = sb.toString();
			ASTParser parser = ASTParser.newParser(AST.JLS3);
			parser.setSource(source.toCharArray());
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			CompilationUnit cu = (CompilationUnit) parser.createAST(null);
			PolicyASTVisitor ast = new PolicyASTVisitor();
			cu.accept(ast);
			return ast.getMtd2Evts();
		}
		catch (Exception ex) { compilationFailure(); }

		return null;
	}

    @NonNull
    private Map<String, ArrayList<String>> analyzeClassFiles(String pname, JarFile jarFiles) {
		Enumeration enumEntries = jarFiles.entries();
		while (enumEntries.hasMoreElements()) {
			JarEntry entry = (JarEntry) enumEntries.nextElement();
			if(entry.isDirectory()) continue;

			String jarFileName = entry.getName();
			String policyName= jarFileName.substring(0,jarFileName.length()-6);
			if(jarFileName.endsWith(".class") && policyName.equals(pname)) {
				PolicyVisitor pv = analyzePolicyClass(jarFiles, entry);
				if (pv != null)
					return pv.getMthName2Evtsigs();
			}
		}
		return null;
    }

	private PolicyVisitor analyzePolicyClass(JarFile jarFiles, JarEntry entry) {
		try(InputStream inputStream = jarFiles.getInputStream(entry)) {
			ClassReader cr = new ClassReader(inputStream);
			ClassNode node = new ClassNode();
			cr.accept(node, ClassReader.EXPAND_FRAMES);
			ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
			PolicyVisitor pv = new PolicyVisitor(api, cw, node);
			cr.accept(pv, api);
			return pv;
		}
		catch (Exception ex) { compilationFailure(); }
		return null;
	}

	private JarFile getJarFiles(String path, String fileName, int resourse) {
		JarFile jarFile = null;
		InputStream is = WordPress.getContext().getResources().openRawResource(resourse);
		File file = createFile(path, fileName);

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(is.available());
			 FileOutputStream outputStream = new FileOutputStream(file, true)) {
			byte[] buffer = new byte[2048];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				baos.write(buffer, 0, bytesRead);
			}
			baos.writeTo(outputStream);
			outputStream.flush();
			jarFile = new JarFile(file);
		} catch (IOException e) {
			Log.e("Aspects", e.getMessage());
            e.printStackTrace();
		}
		try { is.close(); } catch (IOException e) { e.printStackTrace(); }
		return jarFile;
	}

	private File createFile(String path, String name) {
		File dir = new File(path);
		if (!dir.exists())  dir.mkdirs();
		File file = new File(path, name);
		try {
			if (file.exists())
				file.delete();
			file.createNewFile();
		}
		catch (IOException e) { compilationFailure(); }
		return file;
	}

	private ArrayList<String> getPolicyNames(Policy[] policies) {
		ArrayList<String> names = new ArrayList<>();
		if(policies != null && policies.length>0)
			for(Policy p: policies)
				if(p != null)  names.add(p.getClass().getSimpleName());
		return names;
	}

	private void compilationFailure() {
		Log.e(TAG, "Failed to analyze policy files, please check.");
		System.exit(-1);
	}

	private void genCFGs(String policyName, Map<String, ArrayList<EventInfo>> mtd2evtSigs) {
		Map<String, CFG> mtd2cfgs  = new HashMap<>();
		Set<String> mtds = mtd2evtSigs.keySet();
		if(mtds != null && mtds.size() > 0) {
			for(String mtd: mtds) {
				CFG cfg = GenPolicyCFG.genCFG(mtd2evtSigs.get(mtd), policyName, mtd);
				removeFlagTags(cfg);
				if(isEmpty(cfg))
					cfg.setEmptyFlag();
				mtd2cfgs.put(mtd, cfg);
			}
		}
		_poicy2CFGs.put(policyName, mtd2cfgs);
	}

	private boolean isEmpty(CFG root) {
		assert root != null;
		List<CFG> childnodes = root.getChildnodes();
		if(childnodes != null && childnodes.size() == 1) {
			if(childnodes.get(0).getEvent().getSig().equals("END_OF_METHOD"))
				return true;
		}
		return false;
	}

	private void removeFlagTags(CFG root) {
		CFG flagTag = hasFlagTAg(root);
		while(flagTag != null) {
			while(removeFromCFG(root, flagTag)) {
				;
			}
			flagTag = hasFlagTAg(root);
		}
	}

	private boolean removeFromCFG(CFG root, CFG flagTag) {
		if(root == null) return false;

		Queue<CFG> queue = new LinkedList<CFG>();
		queue.offer(root);
		while (!queue.isEmpty()) {
			int currQueueSize = queue.size();
			for(int i = 0; i < currQueueSize; i++) {
				CFG node = queue.poll();
				List<CFG> childNodes = node.getChildnodes();
				if (childNodes != null && childNodes.size() > 0) {
					for(CFG child: childNodes) {
						if(child.equals(flagTag)) {
							node.addChildnodes(flagTag.getChildnodes());
							node.removeChildNode(flagTag);
							return true;
						}
						queue.offer(child);
					}
				}
			}
		}
		return false;
	}

	private CFG hasFlagTAg(CFG root) {
		if(root == null)
			return null;

		Queue<CFG> queue = new LinkedList<CFG>();
		queue.add(root);
		while (!queue.isEmpty()) {
			int currQueueSize = queue.size();
			for(int i = 0; i < currQueueSize; i++) {
				CFG node = queue.poll();
				EventInfo ei = node.getEvent();
				if(ei!= null && ParsFlgConsts.IS_STATEMENT_FLAG(ei.getSig()))
					return node;

				List<CFG> childNodes = node.getChildnodes();
				int childCount = (childNodes == null) ? 0 : childNodes.size();
				if (childCount > 0) {
					for(CFG child: childNodes)
						queue.add(child);
				}
			}
		}
		return null;
	}

	private Map<String, ArrayList<EventInfo>> mergeSigInfo(Map<String, ArrayList<EventInfo>> mtd2evtSigs,
			Map<String, ArrayList<String>> mtd2DetailedEvtSigs) {
		if (mtd2evtSigs == null || mtd2DetailedEvtSigs == null)
			return null;

		Set<String> mtds = mtd2evtSigs.keySet();
		for (String mtd : mtds) {
			ArrayList<EventInfo> evtList = mtd2evtSigs.get(mtd);
			ArrayList<String> detailed = mtd2DetailedEvtSigs.get(mtd);
			merge(evtList, detailed, mtds);
		}
		return null;
	}

	private  void merge(ArrayList<EventInfo> evtList, ArrayList<String> detailed, Set<String> mtds) {
        if (evtList.size() == 0) return;

        int index = 0;
        for (int i = 0; i < evtList.size(); i++) {
            String sig = evtList.get(i).getSig();
            if (ParsFlgConsts.IS_STATEMENT_FLAG(sig)) continue;
            for (; index < detailed.size(); index++) {
                String longSig = getLongSig(detailed.get(index));
                if (isMatching(longSig, sig) || isSystemCalls(longSig, sig) || mtds.contains(sig) ) {
                    evtList.get(i).setSig(longSig);
                    index++;
                    break;
                } else  {
                    //SendEmail.isResolvable();
                    //edu.usfcse.poco.event.Action.isResolvable(null)
                    String mtdName  = RuntimeUtils.getMtdName(sig);
                    String longName = RuntimeUtils.getMtdName(longSig);
                    if( longName.equals(mtdName) ) {
                        String shortPkg = RuntimeUtils.getPackageName(sig);
                        String longPkg  = RuntimeUtils.getPackageName(longSig);
                        if(isObjClassCase(shortPkg,longPkg) || isOtherCase(shortPkg, longPkg) || isSubclassCase(shortPkg,longPkg)){
                            evtList.get(i).setSig(longSig);
                            index++;
                            break;
                        }
                    }
                }
            }
        }
    }

    private String getLongSig(String longSig) {
        if (longSig.startsWith("$"))
            longSig = longSig.split("\\$")[1];
        return longSig.replaceAll("(null)", "");
    }

    private boolean isObjClassCase(String shortPack,String longPack ) {
        return  shortPack == null || shortPack.equals("null") ||
                longPack.equals("java.lang.Object") || shortPack.equals("Object");
    }
    private boolean isSubclassCase(String shortPack,String longPack ) {
	    if(shortPack == null)
	        return false;
        Class clazz1 = null, clazz2 = null;
        try {
            clazz1 = Class.forName(shortPack);
            clazz2 = Class.forName(longPack);
        } catch (ClassNotFoundException e) {}

        return clazz1 != null && clazz2 != null && (clazz2.isAssignableFrom(clazz1) || clazz1.isAssignableFrom(clazz2));
    }
    private boolean isOtherCase(String shortPack,String longPack ) {
	    return shortPack.indexOf('<') > 0 && shortPack.indexOf('>') > 0 &&
                longPack.contains(shortPack.substring(0, shortPack.indexOf('<')));
    }

	private static boolean isSystemCalls(String longSig, String sig) {
		if (sig.startsWith("System.")) {
			switch (sig) {
			case "System.out.print(String)":
			case "System.out.print(java.lang.String)":
			case "System.err.print(String)":
			case "System.err.print(java.lang.String)":
				if (longSig.equals("java.io.PrintStream.print(java.lang.String)"))
					return true;

			case "System.out.println(String)":
			case "System.out.println(java.lang.String)":
			case "System.err.println(String)":
			case "System.err.println(java.lang.String)":
				if (longSig.equals("java.io.PrintStream.println(java.lang.String)"))
					return true;
			default:
				break;
			}
		}
		return false;
	}

	private boolean isMatching(String longSig, String sig) {
		String longSig4Mtd = Utils.getMtdName(longSig);
		String shortSig4Mtd = Utils.getMtdName(sig);

		if (longSig4Mtd.endsWith(shortSig4Mtd)) {
			String[] longPara = Utils.getMethodParas(longSig);
			String[] shortPara = Utils.getMethodParas(sig);
			if(longPara == null || shortPara == null)
			    return false;
            return longPara.length == shortPara.length;
		}
		return false;
	}

	private boolean isRootNode(String sig) {
		return sig != null && sig.equals("RootNode");
	}

	private void preValidation(String currPolicyName, CFG cfg) {
		Set<String> policies = _policy2ConcernedEvts.keySet();
		for (String policy : policies) {
			if (currPolicyName.equals(policy))
				continue;
			check(currPolicyName, cfg, policy);
		}
	}

	private void check(String currPolicyName, CFG cfg, String policy) {
		Set<String> concernedEvts = _policy2ConcernedEvts.get(policy);
		if (concernedEvts.size() == 0)
			return;

		boolean isViolate = false;
		Queue<CFG> queue = new LinkedList<CFG>();
		queue.add(cfg);
		while (!queue.isEmpty() && !isViolate) {
			CFG node = queue.remove();
			if (node.getEvent() != null) {
				String sig = node.getEvent().getSig();
				if (!isRootNode(sig) && !sig.equals("END_OF_METHOD") && !sig.equals("e")) {
					if (matches(concernedEvts, sig)) {
						isViolate = true;
						break;
					}
				}
			}

			List<CFG> childnodes = node.getChildnodes();
			if (childnodes != null && childnodes.size() > 0) {
				for(CFG child: childnodes)
					queue.add(child);
			}
		}
		if (isViolate)
			System.err.println("The policy " + toSimpleName(currPolicyName)
					+ "'s obligations may voildate security concerns of policy " + toSimpleName(policy));
	}

	private String toSimpleName(String pname) {
		if(pname.indexOf('.') !=-1)
			return pname.substring(pname.lastIndexOf('.')+1);
		return pname;
	}
	private static boolean matches(Set<String> concernedEvts, String sig) {
		if (concernedEvts.contains(sig))
			return true;
		else {
			for (String concernedEvt : concernedEvts)
				if (Utils.matchSignature(Utils.validateStr(concernedEvt), sig))
					return true;
		}
		return false;
	}

	public Map<String, Map<String, CFG>> getPoicy2CFGs() { return _poicy2CFGs; }
	public ArrayList<String> getPolicyNames() { return _policyNames;}
	public void setPolicyNames(ArrayList<String> names) { _policyNames = names;}
}