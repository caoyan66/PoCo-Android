package org.wordpress.android.poco.policy.staticAnalysis.scanPolicies;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class PolicyASTVisitor extends ASTVisitor {
    public static final String TAG = PolicyASTVisitor.class.getName();
    private final String EVENT_TYPE  = "edu.usfcse.poco.event.Event";
    private final String ACTION_TYPE = "edu.usfcse.poco.event.Action";
    private final String RESULT_TYPE = "edu.usfcse.poco.event.Result";
    private final String CFG_TYPE    = "edu.usfcse.poco.policy.CFG";
    private final String RTRACE_TYPE = "edu.usfcse.poco.policy.Rtrace";

    private Map<String, VariableObject> _var2TypVal;
    private Stack<String> _currVar4Assignment;
    private Stack<String> _currInvokeMtd;

    private String _currMtdName;
    private LinkedHashMap<String, String> _argsNam2Typ4currMtd;

    private ArrayList<EventInfo> _methodSigs;
    private LinkedHashMap<String, ArrayList<EventInfo>> _mthName2Evtsigs;
    public LinkedHashMap<String, ArrayList<EventInfo>> getMtd2Evts(){
        return _mthName2Evtsigs; }

    private String _policyName;

    public PolicyASTVisitor(){
        initMtdName2Evtsigs();
        _var2TypVal = new HashMap<String, VariableObject>();
        _methodSigs = new ArrayList<EventInfo>();
        _currVar4Assignment = new Stack<String>();
        _currInvokeMtd = new Stack<String>();
    }

    private void addNewMethNode(String mtdName) {
        _methodSigs = new ArrayList<>();
        _mthName2Evtsigs.put(mtdName, _methodSigs);
    }

    private void initMtdName2Evtsigs() {
        _mthName2Evtsigs = new LinkedHashMap<String, ArrayList<EventInfo>>();
        _mthName2Evtsigs.put("onTrigger", new ArrayList<EventInfo>());
        _mthName2Evtsigs.put("vote", new ArrayList<EventInfo>());
        _mthName2Evtsigs.put("onOblig", new ArrayList<EventInfo>());
    }

    public boolean visit(TypeDeclaration node) {
        if(node.getSuperclassType() == null)
            return false;
        String superClassType = node.getSuperclassType().toString();
        if (superClassType != null && superClassType.equals("Policy")) {
            _policyName = node.getName().getFullyQualifiedName();
            return super.visit(node);
        }else
            return false;
    }

    //=============== Visit FieldDeclaration ===============
    public boolean visit(FieldDeclaration node) {
        //TODO: arrayType?
        String fieldType = convertTypes(node.getType().toString());
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) node.fragments().get(0);
        String varName = fragment.getName().toString();
        _currVar4Assignment.push(varName);
        VariableObject var = new VariableObject(fieldType, null, false);
        _var2TypVal.put(varName, var);
        return super.visit(node);
    }
    public void endVisit(FieldDeclaration node) {
        if(!_currVar4Assignment.isEmpty())
            _currVar4Assignment.pop();
    }

    //=============== Visit MethodDeclaration ===============
    public boolean visit(MethodDeclaration node) {
        _argsNam2Typ4currMtd = new LinkedHashMap<>();
        if( !node.isConstructor() && !Modifier.isStatic(node.getModifiers()) ) {
            _currMtdName = node.getName().toString();
            addNewMethNode(_currMtdName);
            List parameters = node.parameters();
            if (parameters != null && parameters.size() > 0) {
                for (Object parameter : parameters) {
                    String paraType = parameter.getClass().getSimpleName();
                    switch (paraType) {
                        case "SingleVariableDeclaration":
                            addVar((SingleVariableDeclaration) parameter);
                            break;
                        default:
                            break;
                    }
                }
            }
            return super.visit(node);
        }
        return false;
    }
    public void endVisit(MethodDeclaration node) {  _currMtdName = ""; }

    private void addVar(SingleVariableDeclaration singleVar) {
        String varType = convertTypes(singleVar.getType().toString());
        _argsNam2Typ4currMtd.put(singleVar.getName().toString(), varType);
    }
    public boolean visit(WhileStatement node) {
        handleWhileStatement(node);
        return false;
    }

    //=============== Visit IfStatement ===============
    public boolean visit(IfStatement node) {
        handleIfStatement(node);
        return false;
    }

    public boolean visit(MethodInvocation node) {
        handleMtdInvokeCase4Expr((MethodInvocation) node);
        return false;
    }

    public boolean visit(SwitchStatement node) {
        handleSwitchStatement(node);
        return false;
    }

    private void handleStatements(List<Statement> statements) {
        if(statements != null && statements.size() > 0)
            for(Statement st: statements) handleStatement(st);
    }

    private void handleStatement(Statement statement) {
        if(statement == null) return;

        switch (statement.getNodeType()) {
            case ASTNode.BLOCK:
                Block block = (Block)statement;
                handleStatements(block.statements());
                break;
            case ASTNode.EXPRESSION_STATEMENT:
                ExpressionStatement es = (ExpressionStatement)statement;
                handleExpression(es.getExpression(), false);
                break;
            case ASTNode.VARIABLE_DECLARATION_STATEMENT:
                visitVariableDeclarationStatement((VariableDeclarationStatement) statement);
                break;
            case ASTNode.IF_STATEMENT:
                handleIfStatement((IfStatement)statement);
                break;
            case ASTNode.SWITCH_STATEMENT:
                handleSwitchStatement((SwitchStatement)statement);
                break;
            case ASTNode.TRY_STATEMENT:
                handleTryStatement((TryStatement)statement);
                break;
            case ASTNode.RETURN_STATEMENT:
                ReturnStatement rs = (ReturnStatement)statement;
                handleExpression(rs.getExpression(), false);
                break;
            case ASTNode.ASSIGNMENT:
                ExpressionStatement ess = ( ExpressionStatement) statement;
                handleExpression(ess.getExpression(),false);
                break;
            case ASTNode.WHILE_STATEMENT:
                handleWhileStatement((WhileStatement) statement);
                break;
            case ASTNode.CAST_EXPRESSION:
                ExpressionStatement exp = (ExpressionStatement) statement;
                handleExpression(exp.getExpression(),false);
                break;
            case ASTNode.ENHANCED_FOR_STATEMENT:
                handleEnhancedFor((EnhancedForStatement) statement);
                break;
            case ASTNode.FOR_STATEMENT:
                handleFor((ForStatement) statement);
                break;
            case ASTNode.BREAK_STATEMENT:
            case ASTNode.SWITCH_CASE:
            case ASTNode.SIMPLE_NAME:
            default:
                break;
        }
    }

    public boolean visit(EnhancedForStatement node) {
        handleEnhancedFor(node);
        return false;
    }

    public boolean visit(ForStatement node) {
        handleFor(node);
        return false;
    }

    private void handleFor(ForStatement node) {
        _methodSigs.add(new EventInfo(ParsFlgConsts.FOR));
        // step 1: handle for initializer
        List<Expression> inits = node.initializers();
        for(Expression init: inits)
            handleExpression(init,false);
        // step 2: handle for condition
        _methodSigs.add(new EventInfo(ParsFlgConsts.FOR_CONDITION));
        handleExpression(node.getExpression(),false);

        _methodSigs.add(new EventInfo(ParsFlgConsts.FOR_BLOCK));
        handleStatement(node.getBody());
        _methodSigs.add(new EventInfo(ParsFlgConsts.END_FOR));
    }

    private void handleEnhancedFor(EnhancedForStatement enfor) {
        addVar(enfor.getParameter());
        _methodSigs.add(new EventInfo(ParsFlgConsts.ENHANCED_FOR));
        handleExpression(enfor.getExpression(),false);
        _methodSigs.add(new EventInfo(ParsFlgConsts.ENHANCED_FOR_BLOCK));
        handleStatement(enfor.getBody());
        _methodSigs.add(new EventInfo(ParsFlgConsts.END_ENHANCED_FOR));
    }

    private void handleWhileStatement(WhileStatement statement) {
        _methodSigs.add(new EventInfo(ParsFlgConsts.WHILE));
        handleExpression(statement.getExpression(),false);

        _methodSigs.add(new EventInfo(ParsFlgConsts.WHILE_BLOCK));
        handleStatement(statement.getBody());

        _methodSigs.add(new EventInfo(ParsFlgConsts.END_WHILE));
    }

    private void handleIfStatement(IfStatement statement) {
        _methodSigs.add(new EventInfo(ParsFlgConsts.IF_CONDITION));
        handleExpression(statement.getExpression(), false);

        _methodSigs.add(new EventInfo(ParsFlgConsts.THEN_BRANCH));
        handleStatement(statement.getThenStatement());

        Statement elseStatement = statement.getElseStatement();
        if (elseStatement != null) {
            _methodSigs.add(new EventInfo(ParsFlgConsts.ELSE_BRANCH));
            handleStatement(elseStatement);
        }
        _methodSigs.add(new EventInfo(ParsFlgConsts.ENDOFIF));
    }

    private void handleSwitchStatement(SwitchStatement statement) {
        _methodSigs.add(new EventInfo(ParsFlgConsts.IF_CONDITION));
        handleExpression(statement.getExpression(), false);

        boolean isFirstCaseStatement = true;
        int elseCount = 0;
        List<Statement> sts = statement.statements();
        for(Statement st: sts) {
            switch (st.getNodeType()) {
                case ASTNode.SWITCH_CASE:
                    if(isFirstCaseStatement) {
                        isFirstCaseStatement = false;
                        _methodSigs.add(new EventInfo(ParsFlgConsts.THEN_BRANCH));
                    }
                    else {
                        elseCount++;
                        _methodSigs.add(new EventInfo(ParsFlgConsts.ELSE_BRANCH));
                        _methodSigs.add(new EventInfo(ParsFlgConsts.IF_CONDITION));
                        _methodSigs.add(new EventInfo(ParsFlgConsts.THEN_BRANCH));
                    }
                    break;
                case ASTNode.BREAK_STATEMENT:
                    break;
                default:
                    handleStatement(st);
            }
        }
        for(int i = 0; i< elseCount; i++)
            _methodSigs.add(new EventInfo(ParsFlgConsts.ENDOFIF));
    }

    public boolean visit(VariableDeclarationStatement node) {
        visitVariableDeclarationStatement(node);
        return false;
    }

    private void visitVariableDeclarationStatement(VariableDeclarationStatement vds) {
        Type type = vds.getType();
        String varType = type.toString();
        if(type.isParameterizedType()) {
            ParameterizedType pt = (ParameterizedType) type;
            varType = pt.getType().toString();
        }

        List fragments = vds.fragments();
        if(fragments != null && fragments.size() > 0) {
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.get(0);
            String currVarName = fragment.getName().toString();
            // step 1. handle the left side of the assignment
            _currVar4Assignment.push(currVarName);

            // step 2. handle the right hand side of the assignment
            Expression init = fragment.getInitializer();
            if(init != null) {
                VariableObject var = null;
                switch (init.getNodeType()) {
                    case Expression.CLASS_INSTANCE_CREATION:
                        var = handleNewClass4Var(varType, init); break;
                    case Expression.STRING_LITERAL:
                        String val = init.toString();
                        if(val.length() > 1 && val.startsWith("\"") && val.endsWith("\""))
                            val = val.substring(1, val.length()-1);
                        var = new VariableObject("java.lang.String",val , true);
                        break;
                    case Expression.METHOD_INVOCATION:
                        handleExpression(init, false);
                        var = new VariableObject(varType, null, false);
                        break;
                    case Expression.NULL_LITERAL:
                        var = new VariableObject(varType, null, true); break;
                    case Expression.SIMPLE_NAME:
                        String argName = init.toString();
                        if (_var2TypVal.containsKey(argName))  var = _var2TypVal.get(argName);
                        break;
                    case Expression.CAST_EXPRESSION:
                        CastExpression cast = (CastExpression) init;
                        handleExpression(cast.getExpression(),false);
                        break;
                    default:
                        var = new VariableObject(varType, null, false);
                        break;
                }
                _var2TypVal.put(currVarName, var);
            }
            else {
                // variable is not initialized
                if(_methodSigs.size() >0 ){
                    EventInfo temp = _methodSigs.get(_methodSigs.size()-1);
                    if(ParsFlgConsts.isCatchStatement(temp.getSig()))
                        temp.setProperty(varType); //add exception type to catch flag
                }
                VariableObject vObject = new VariableObject(varType, null, true);
                _var2TypVal.put(currVarName, vObject);
            }
            // pop the flag
            _currVar4Assignment.pop();
        }
    }

    public boolean visit(ReturnStatement node) {
        handleExpression(node.getExpression(), false);
        return false;
    }

    public boolean visit(TryStatement node) {
        handleTryStatement(node);
        return false;
    }

    private void handleTryStatement(TryStatement node) {
        if (node != null) {
            _methodSigs.add(new EventInfo(ParsFlgConsts.TRY));
            // step 1. handle try block
            //_methodSigs.add(new EventInfo(ParsFlgConsts.TRY_BLOCK));
            handleStatements(node.getBody().statements());
            // step 2. handle catch blocks
            handleCatchClauses(node.catchClauses());
            // step 3. handle final blocks
            if (node.getFinally() != null) {
                _methodSigs.add(new EventInfo(ParsFlgConsts.TRY_FINAL));
                handleStatement(node.getFinally());
            }
            _methodSigs.add(new EventInfo(ParsFlgConsts.ENDTRY));
        }
    }

    private void handleCatchClauses(List<CatchClause> catchClauses) {
        if(catchClauses != null && catchClauses.size()>0) {
            _methodSigs.add(new EventInfo(ParsFlgConsts.CATCH_BLOCK));
            for(CatchClause cc: catchClauses) {
                //1. add variable declartion to dictionary
                addVar(cc.getException());
                //2. handle statements in catch clauses
                handleStatements(cc.getBody().statements());
            }
        }
    }

    private VariableObject handleNewClass4Var(String varType, Expression init) {
        VariableObject var;
        ClassInstanceCreation cic = (ClassInstanceCreation)init;
        var = new VariableObject(varType, null, true);

        switch (varType) {
            case "Action":
            case "Result":
                String realType = cic.getType().toString();
                var = new VariableObject(realType, null, true);
                MtdArgs args = handleVarArgs(cic.arguments());
                var.setVarArgs(args);
                String argStr = genArgStr(args);
                addEvtSig2List(convertTypes(realType) + ".<init>(" + argStr + ")", args, true);
                break;

            default:
                MtdArgs mtdArgs = handleVarArgs(cic.arguments());
                var.setVarArgs(mtdArgs);
                addEvtSig2List(convertTypes(varType) + ".<init>(" + genArgStr(mtdArgs) + ")", mtdArgs, true);
                break;
        }
        return var;
    }

    private void handleExpression(Expression exp, boolean handlingMtdArg) {
        if(exp == null)  return;

        switch (exp.getNodeType()) {
            case Expression.INFIX_EXPRESSION:
                handleInfixExp((InfixExpression) exp, handlingMtdArg);
                break;
            case Expression.PARENTHESIZED_EXPRESSION:
                ParenthesizedExpression parenthesized = (ParenthesizedExpression) exp;
                handleExpression(parenthesized.getExpression(), handlingMtdArg);
                break;
            case Expression.METHOD_INVOCATION: //e.g., e.matches(getDeviceId) or outputNotSet()
                handleMtdInvokeCase4Expr((MethodInvocation) exp);
                break;
            case Expression.CLASS_INSTANCE_CREATION:
                handleNewCase4Expr((ClassInstanceCreation)exp);
                break;
            case Expression.PREFIX_EXPRESSION:
                PrefixExpression preExp = (PrefixExpression) exp;
                handleExpression(preExp.getOperand(), handlingMtdArg);
                break;
            case Expression.ASSIGNMENT:
                Assignment asExp = (Assignment) exp;
                handleExpression(asExp.getRightHandSide(), handlingMtdArg);
                break;
            case Expression.CAST_EXPRESSION:
                CastExpression cast = (CastExpression) exp;
                handleExpression(cast.getExpression(), handlingMtdArg);
                break;
            case Expression.SIMPLE_NAME:
            case Expression.NUMBER_LITERAL:
            case Expression.NULL_LITERAL:
            case Expression.BOOLEAN_LITERAL:
            case Expression.STRING_LITERAL:
            default:
                break;
        }
    }

    private void handleNewCase4Expr(ClassInstanceCreation cic) {
        //ClassInstanceCreation: new InputStreamReader(r.exec("top -n 1").getInputStream())
        MtdArgs mtdArgs = handleVarArgs(cic.arguments());
        addEvtSig2List(cic.getType() + ".<init>(" + genArgStr(mtdArgs) + ")", mtdArgs, true);
    }

    private void handleInfixExp(InfixExpression infix, boolean isArg){
        handleExpression(infix.getLeftOperand(), isArg);
        handleExpression(infix.getRightOperand(), isArg);
        handleInfixExtendedOperands(infix.extendedOperands(), isArg);
    }

    private void handleInfixExtendedOperands(List<Expression> exps, boolean isArg) {
        if(exps != null && exps.size() > 0) {
            for(Expression exp: exps)
                handleExpression(exp,isArg);
        }
    }

    /**
     * handle the Method invocation case, e.g., e.matches(getDeviceId) or outputNotSet()
     * @param exp
     */
    private void handleMtdInvokeCase4Expr(MethodInvocation exp) {
        // step 1. get the invoked method's name and push it onto the stack
        String mtdname = exp.getName().toString(); // e.g. matches
        _currInvokeMtd.push(mtdname);

        // step 2. handle caller
        Expression callerExp = exp.getExpression();
        handleExpression(callerExp,false);
        MtdArgs caller = getmtdInvokeCaller(callerExp);
        String callerType = (caller == null) ? "" : caller.getArgTyps()[0] + ".";

        // step 2. handle arguments for METHOD_INVOCATION (arguments can still be methodInvocation)
        List<Expression> args = exp.arguments();
        MtdArgs mtdArgs = handleVarArgs(args);

        // step 3. generate valid method name and add it to the list
        boolean isEvtResolvable = checkResolvable(args);
        //TODO: handle this case
        //if(mtdName.startsWith("this.")) mtdName = mtdName.substring(5);
        String mtdSig = callerType + mtdname + "(" + handleArugmentSig(args) + ")";
        if(isVisitingPolicyOVO() || (_currMtdName != null && !_currMtdName.equals("<init>")))
            _methodSigs.add(new EventInfo(mtdSig, mtdArgs, isEvtResolvable, caller));
        // step 4: pop the current invoking method name
        _currInvokeMtd.pop();
    }

    private String handleArugmentSig(List<Expression> args) {
        if (args == null) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            switch (args.get(i).getNodeType()) {
                case Expression.SIMPLE_NAME:
                    sb.append( handleId(args.get(i).toString()) ); break;
                case Expression.NULL_LITERAL:
                case Expression.METHOD_INVOCATION:
                    sb.append("NULL");
                    break;
                case Expression.CLASS_INSTANCE_CREATION:
                    ClassInstanceCreation cic = (ClassInstanceCreation) args.get(i);
                    sb.append(convertTypes(cic.getType().toString()));
                    break;
                case Expression.BOOLEAN_LITERAL:
                    sb.append("boolean");   break;
                case Expression.NUMBER_LITERAL:
                    sb.append("int");       break;
                case Expression.STRING_LITERAL:
                    sb.append("java.lang.String");  break;
                case Expression.INITIALIZER:
//                    sb.append( newClassTree.getIdentifier().toString() );
                    break;
                default:
                    sb.append("NULL");        break;
            }

            if (i != args.size() - 1) sb.append(",");
        }
        return sb.toString();
    }

    private String handleId(String argName) {
        if (_var2TypVal.containsKey(argName)) {
            VariableObject argObj = _var2TypVal.get(argName);
            return argObj.getVartype();
        } else
            return checkMtdArgs(argName);
    }

    private String checkMtdArgs(String argName) {
        if(_argsNam2Typ4currMtd != null && !_argsNam2Typ4currMtd.isEmpty()) {
            Set<String> vars = _argsNam2Typ4currMtd.keySet();
            for(String var: vars) {
                if(var.equals(argName))
                    return _argsNam2Typ4currMtd.get(var);
            }
        }
        return null;
    }

    private MtdArgs handleVarArgs(List<Expression> args) {
        if (args == null || args.isEmpty()) return null;

        boolean resolveable = true;
        Object[] obj4Arg   = new Object[args.size()];
        String[] argType = new String[args.size()];
        boolean[] isTrigEvt = new boolean[args.size()]; Arrays.fill(isTrigEvt, false);

        for (int i = 0; i < args.size(); i++) {
            switch (args.get(i).getNodeType()) {
                case Expression.STRING_LITERAL:
                    String val = args.get(i).toString();
                    obj4Arg[i] = val.substring(1, val.length()-1);
                    argType[i] = "java.lang.String";
                    break;

                case Expression.SIMPLE_NAME:
                    resolveable = handleIdCase4arg(args, obj4Arg, argType, isTrigEvt, i) && resolveable;
                    break;

                case Expression.NUMBER_LITERAL:
                    obj4Arg[i] = new Integer(args.get(i).toString());
                    argType[i] = "int";
                    break;

                case Expression.CLASS_INSTANCE_CREATION:
                    isTrigEvt[i] = false;
                    ClassInstanceCreation cic = (ClassInstanceCreation)args.get(i);
                    argType[i] = cic.getType().toString();
                    MtdArgs arguments = handleVarArgs(cic.arguments());
                    String sig = convertTypes(argType[i]) + ".<init>(" + genArgStr(arguments) + ")";
                    addEvtSig2List(sig, arguments, false);
//                    if(isPrimitiveTyp(argType[i])) {
//                        obj4Arg[i]  = getPrimitiveValue(argType[i], cic.arguments());
//                       // resolveable = (tree.getArguments() != null);
//                    }else {
//                        obj4Arg[i]  = arguments;
//                        resolveable = (obj4Arg[i] == null) ? true: arguments.isResolvable();
//                    }
                    break;
                case Expression.BOOLEAN_LITERAL:
                    obj4Arg[i] = new Boolean(args.get(i).toString());
                    argType[i] = "boolean";
                    break;
                case Expression.METHOD_INVOCATION:
                    handleExpression(args.get(i),true);
                    argType[i] = "UNKNOWN";
                    resolveable = false;
                    break;
                case Expression.INFIX_EXPRESSION:
                    InfixExpression infix = (InfixExpression) args.get(i);
                    argType[i] = getType4InfixExp(infix);
                    handleExpression(args.get(i),true);
                    break;
                case Expression.NULL_LITERAL:
                case Expression.QUALIFIED_NAME:  //fakeInfo.PHONE_NUMBER
                    argType[i]  = "Object";
                    break;
                default:
                    break;
            }
        }
        return new MtdArgs(obj4Arg, argType, isTrigEvt, resolveable);
    }

    private String getType4InfixExp(InfixExpression infix) {
        InfixExpression.Operator op = infix.getOperator();
        switch (op.toString()) {
            case "<":
            case ">":
            case "<=":
            case ">=":
            case "==":
            case "!=":
            case "&":
            case "|":
            case "&&":
            case "||":  return "boolean";
            default:    return "UNKNOWN";
        }
    }

    private MtdArgs getmtdInvokeCaller(Expression exp) {
        MtdArgs caller = null;
        if (exp == null) return caller;

        boolean[] isTrig = {false};
        String varType = null;
        Object varObj = null;

        switch (exp.getNodeType()) {
            case Expression.SIMPLE_NAME:
                String varName = exp.toString();
                if( _var2TypVal.containsKey(varName) ) {
                    if(_var2TypVal.get(varName) != null) {
                        varType = _var2TypVal.get(varName).getVartype();
                        varObj = _var2TypVal.get(varName).getVarVal();
                    }
                }else if( _argsNam2Typ4currMtd.containsKey(varName) ) {
                    if(_argsNam2Typ4currMtd.get(varName) != null)
                        varType = _argsNam2Typ4currMtd.get(varName);
                }
                else if( visitingOnTriggerMtd()  && varName.equals(arg4PoCoMtd()) ) {
                    isTrig[0] = true;
                    varType = EVENT_TYPE;
                }
                else if( visitingVoteMtd() && varName.equals(arg4PoCoMtd()) ) {
                    varType = CFG_TYPE;
                }
                else if( visitingOnObligMtd() && varName.equals(arg4PoCoMtd()) ) {
                    varType = RTRACE_TYPE;
                }
                else { //static method case
                    varType = exp.toString();
                }
                caller = new MtdArgs(new Object[]{varObj},new String[]{varType}, isTrig);
                break;
            case Expression.PARENTHESIZED_EXPRESSION:
                ParenthesizedExpression pe = (ParenthesizedExpression)exp;
                caller =  getmtdInvokeCaller(pe.getExpression());
                break;
            case Expression.CAST_EXPRESSION:
                CastExpression ce = (CastExpression)exp;
                caller = new MtdArgs(new Object[]{varObj},new String[]{ce.getType().toString()}, isTrig);
                break;
            case Expression.METHOD_INVOCATION:
                caller = new MtdArgs(new Object[]{null}, new String[]{"Object"}, new boolean[]{false});
                break;
            default:
                break;
        }
        return caller;
    }

    private boolean checkResolvable(List args) {
        if (args == null)
            return true;
        for(Object arg: args) {
            switch (arg.getClass().getSimpleName()) {
                case "SimpleName":
                    String argName = arg.toString();
                    if (_var2TypVal.containsKey(argName)) {
                        VariableObject argObj = _var2TypVal.get(argName);
                        if (!argObj.isResolvable()) return false;
                    }
                    else  return false;
                    break;
                default: break;
            }
        }
        return true;
    }

    private boolean handleIdCase4arg(List args, Object[] obj4Arg, String[] argType, boolean[] isTrigEvt, int i) {
        boolean resolveable = true;
        String varName = args.get(i).toString();

        if( _var2TypVal.containsKey(varName) ) {
            if(_var2TypVal.get(varName) != null) {
                obj4Arg[i] = _var2TypVal.get(varName).getVarVal();
                argType[i] = _var2TypVal.get(varName).getVartype();
                resolveable = _var2TypVal.get(varName).isResolvable();
            }else
                resolveable = false;
        }
        else if( visitingOnTriggerMtd()  && varName.equals(arg4PoCoMtd()) ) {
            argType[i] = EVENT_TYPE;
            isTrigEvt[i] = true;
        }
        else if( visitingVoteMtd() && varName.equals(arg4PoCoMtd()) ) {
            argType[i] = CFG_TYPE;
        }
        else if( visitingOnObligMtd() && varName.equals(arg4PoCoMtd()) ) {
            argType[i] = RTRACE_TYPE;
        }
        return resolveable;
    }

    private void addEvtSig2List(String mtdSig, MtdArgs args, boolean isEvtResolvable) {
        if(isVisitingPolicyOVO() || (_currMtdName != null && !_currMtdName.equals("<init>")))
            _methodSigs.add(new EventInfo(mtdSig, args, isEvtResolvable, null));
    }

    private boolean isVisitingPolicyOVO() {
        return visitingOnTriggerMtd() || visitingVoteMtd() || visitingOnObligMtd();
    }
    private boolean visitingOnTriggerMtd()  {
        if ( matchingMtd("onTrigger") && _argsNam2Typ4currMtd.size() == 1) {
            String varName = getCurrMtdArgSet()[0];
            String argTyp = _argsNam2Typ4currMtd.get(varName);
            return argTyp.equals("Event") || argTyp.equals(EVENT_TYPE);
        }
        return false;
    }
    private boolean visitingVoteMtd() {
        return matchingMtd("vote") && matchingArgs(1, new String[] {"CFG"});
    }
    private boolean visitingOnObligMtd()  {
        return matchingMtd("onOblig") && matchingArgs(1, new String[] {"Rtrace"});
    }

    private boolean matchingMtd(String mtdName) {
        return _currMtdName!= null && _currMtdName.equals(mtdName);
    }

    private String[] getCurrMtdArgSet() {
        assert _argsNam2Typ4currMtd != null;
        Set<String> keys = _argsNam2Typ4currMtd.keySet();
        return  keys.toArray(new String[keys.size()]);
    }

    private boolean matchingArgs(int length, String[] varTyps) {
        assert varTyps != null && varTyps.length == length;
        if(_argsNam2Typ4currMtd.size() == length) {
            boolean isMatch = true;
            String[] vars = getCurrMtdArgSet();

            for(int i = 0; i<vars.length; i++) {
                String varType = _argsNam2Typ4currMtd.get(vars[i]);
                if(!varType.equals(varTyps[i])) {
                    isMatch = false;
                    break;
                }
            }
            return isMatch;
        }
        return false;
    }

    private String arg4PoCoMtd() {
        Set<String> keys= _argsNam2Typ4currMtd.keySet();
        String[] vars = keys.toArray(new String[keys.size()]);
        return vars[0];
    }

    private String genArgStr(MtdArgs args) {
        if (args == null || args.getArgs() == null)     return "";

        String[] argTyps =args.getArgTyps();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < argTyps.length; i++)
            sb.append(argTyps[i]+ ",");
        return sb.substring(0, sb.length() - 1);
    }

    private String convertTypes(String type) {
        switch(type){
            case "String": return "java.lang.String";
            case "Action": return ACTION_TYPE;
            case "Result": return RESULT_TYPE;
            case "Event":  return EVENT_TYPE;
            default:       return type;
        }
    }
}