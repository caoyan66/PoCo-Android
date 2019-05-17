package org.wordpress.android.poco.policy.staticAnalysis.visitClasses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.wordpress.android.poco.policy.Utils;
import org.wordpress.android.poco.policy.staticAnalysis.scanPolicies.VariableObject;


public class PolicyVisitor extends ClassVisitor {
	private final int api; 
	private String _visitingClzName;
	private List<MethodNode> _methodNodes;
	private Map<String, VariableObject> _closure;
	private Map<String, ArrayList<String>> _mthName2EvtLists;
		
	public PolicyVisitor(Integer api, ClassVisitor cv, ClassNode cn) {
        super(api, cv);
        this.api =  api;
        _methodNodes=cn.methods;
        _closure = new HashMap<String, VariableObject>();
		_mthName2EvtLists = new HashMap<String, ArrayList<String>>();
    }
	
	@Override
	 public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		 if(superName.equals("edu/usfcse/poco/policy/Policy")) {
			 _visitingClzName = Utils.formatClassName(name);
			 super.visit(version, access, name, signature, superName, interfaces);
		 }
	 } 
 
	 @Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if(!name.equals("<init>")) {
			ArrayList<String> mtdSigs = new ArrayList<>();
			_mthName2EvtLists.put(name,mtdSigs);
			return new PolicyMethodVisitor(mtdSigs);
		}
		return null;
	}
	 
	//getters and setters
	public int getApi() { return api;}
	public String getClzName () { return _visitingClzName; }
	public Map<String, VariableObject> get_closure() { return _closure; }
	public Map<String, ArrayList<String>> getMthName2Evtsigs() { return _mthName2EvtLists; }

	class PolicyMethodVisitor extends MethodVisitor {
		private ArrayList<String> _mtdSigs;

		public PolicyMethodVisitor(ArrayList<String> sigs) {
			super(Opcodes.ASM5);
			_mtdSigs = sigs;
		}

		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			String sig = Utils.formatClassName(owner) + "." + name + "("+ Utils.formatArgList(desc) +")";
			_mtdSigs.add(sig);
		}
	}
}

