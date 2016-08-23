package encap;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class EncapDFVUtils {

	static Set<CGNode> handle(EncapDFV dfv, Expression exp) {
		return handle(dfv.cg, exp);
	}
	
	static Set<CGNode> handle(ConnectionGraph cg, Expression exp) {
		if (exp instanceof Assignment) {
			return handle(cg, (Assignment) exp);
		}
		if (exp instanceof MethodInvocation) {
			return handle(cg, (MethodInvocation) exp);
		}
		if (exp instanceof ClassInstanceCreation) {
			return handle(cg, (ClassInstanceCreation) exp);
		}
		if (exp instanceof Name) {
			return handle(cg, (Name) exp);
		}
		if (exp instanceof ThisExpression) {
			return handle(cg, (ThisExpression) exp);
		}
		if (exp instanceof FieldAccess) {
			return handle(cg, (FieldAccess) exp);
		}
		if (exp instanceof SuperFieldAccess) {
			return handle(cg, (SuperFieldAccess) exp);
		}
		if (exp instanceof CastExpression) {
			return handle(cg, ((CastExpression) exp).getExpression());
		}
		if (exp instanceof ParenthesizedExpression) {
			return handle(cg, ((ParenthesizedExpression) exp).getExpression());
		}
		if (exp instanceof ArrayCreation) {
			
		}
		if (exp instanceof ArrayAccess) {
			
		}
		return null;
	}
	
	static Set<CGNode> handle(EncapDFV dfv, Assignment assignment) {		
		return handle(dfv.cg, assignment);
	}
	
	static Set<CGNode> handle(ConnectionGraph cg, Assignment assignment) {
		//Utils.println(">>>" + assignment);
		Expression left = assignment.getLeftHandSide();
		Expression right = assignment.getRightHandSide();
		
		return handle(cg, left, right);
	}
	
	static Set<CGNode> handle(EncapDFV dfv, Expression left, Expression right) {
		return handle(dfv.cg, left, right);
	}
	
	static Set<CGNode> handle(ConnectionGraph cg, Expression left, Expression right) {
		Set<CGNode> leftNodes = handle(cg, left);
		Set<CGNode> rightNodes = handle(cg, right);
		if (leftNodes == null || rightNodes == null)
			return null;
		
		for (CGNode leftNode : leftNodes) {
			for (CGNode rightNode : rightNodes) {
				if (leftNode instanceof RefNode) {
					if (rightNode instanceof RefNode)
						cg.addDeferredEdge(leftNode, rightNode);
					else
						cg.addPointsToEdge(leftNode, (ObjectNode) rightNode);
				} else {
					CommonUtils.printlnError("Error: left side of the assignment is not RefNode.");
				}
			}
		}
	
		return leftNodes;
	}

	static Set<CGNode> handle(EncapDFV dfv, MethodInvocation mti) {
		return handle(dfv.cg, mti);
	}
	
	static Set<CGNode> handle(ConnectionGraph cg, MethodInvocation mti) {
		return cg.map(mti);
	}

	static Set<CGNode> handle(EncapDFV dfv, ClassInstanceCreation cic) {
		return handle(dfv.cg, cic);
	}
	
	static Set<CGNode> handle(ConnectionGraph cg, ClassInstanceCreation cic) {
		//Utils.println(">>>" + cic);
		Set<CGNode> ret = CommonUtils.newHashSet();
		ret.add(cg.getOrAddJavaObjectNode(cic));
		return ret;
	}

	static Set<CGNode> handle(EncapDFV dfv, Name name) {
		return handle(dfv.cg, name);
	}
	
	static Set<CGNode> handle(ConnectionGraph cg, Name name) {
		if (name instanceof SimpleName)
			return handle(cg, (SimpleName)name);
		else
			return handle(cg, (QualifiedName)name);
	}
	
	// return nodes that correspond to SimpleName
	static Set<CGNode> handle(EncapDFV dfv, SimpleName simpleName) {
		return handle(dfv.cg, simpleName);
	}
	
	static Set<CGNode> handle(ConnectionGraph cg, SimpleName simpleName) {
		// for primitive types
		if (CommonUtils.isPrimitive(simpleName) == null || CommonUtils.isPrimitive(simpleName))
			return null;
		
		Set<CGNode> nodes = CommonUtils.newHashSet();
		
		// if simpleName is a class name
		// copy the GlobalEscape part into current CG
		ITypeBinding itb = CommonUtils.getTypeBinding(simpleName);
		if (itb != null) {
			String qualifiedName = itb.getQualifiedName();
			ASTNode astnode = Demo.project.getClassByName(qualifiedName);
			
			if (astnode instanceof TypeDeclaration) {
				TypeDeclaration tpd = (TypeDeclaration) astnode;
				ClassCG ccg = ClassCG.tpd2ccg.get(tpd);
				
				// merge
				if (ccg != null)
					cg.merge(ccg.getClassCG(), EscapeState.GlobalEscape);
			}
			
			return nodes;
		}
		
		// if simpleName is non-primitive
		CGNode node = null;
		if (CommonUtils.isField(simpleName)) {
			TypeDeclaration actualTpd = getActualDeclaringTpd(simpleName, ClassCG.currTpd);
			if (actualTpd == null)
				return null;
			node = cg.getOrAddJavaObjectNode(actualTpd).getFieldNode(simpleName);
			if (node == null)
				CommonUtils.debug("filed node not found!");
			else
				nodes.add(node);
		} else if (CommonUtils.isParameter(simpleName)) {
			node = cg.getParamVarNode(simpleName);
			if (node == null)
				CommonUtils.debug("parameter node not found!");
			else
				nodes.add(node);
		} else {
			node = cg.getOrAddLocalVarNode(simpleName);
			if (node == null)
				CommonUtils.debug("local var node not found!");
			else
				nodes.add(node);
		}
		return nodes;
	}
	
	static TypeDeclaration getActualDeclaringTpd(SimpleName fieldName, TypeDeclaration currTpd) {
		for (FieldDeclaration fieldDec : currTpd.getFields()) {
			@SuppressWarnings("unchecked")
			List<VariableDeclarationFragment> vdfs = fieldDec.fragments();
			for (VariableDeclarationFragment vdf : vdfs) {
				SimpleName vdfName = vdf.getName();
				if (vdfName.subtreeMatch(new ASTMatcher(), fieldName))
					return currTpd;
			}
		}
		
		String currentTpdName = currTpd.resolveBinding().getQualifiedName();
		String outerTpdName = Demo.project.getOuterClassName(currentTpdName);
		if (outerTpdName != null)
			return getActualDeclaringTpd(fieldName, (TypeDeclaration) Demo.project.getClassByName(outerTpdName));
		
		return null;
	}
	
	static Set<CGNode> handle(EncapDFV dfv, QualifiedName qn) {
		return handle(dfv.cg, qn);
	}
	
	static Set<CGNode> handle(ConnectionGraph cg, QualifiedName qn) {
		if (qn == null)
			return null;
		
		SimpleName field = qn.getName();
		if (CommonUtils.isPrimitive(field) == null || CommonUtils.isPrimitive(field))
			return null;

		Set<ObjectNode> ptNodes = null;	
		Set<CGNode> refs = CommonUtils.newHashSet();
		
		Name qualifierName = qn.getQualifier();
		if (qualifierName instanceof SimpleName) {
			SimpleName simpleName = (SimpleName)qualifierName;
			
			Set<CGNode> nodes = handle(cg, simpleName);
			CGNode qualifierNode = null; 
			// if QualifierName is a class name
			// lookup directly
			if (nodes == null || nodes.size() == 0) {
				
				ITypeBinding itb = CommonUtils.getTypeBinding(simpleName);
				if (itb != null) {
					String qualifiedName = itb.getQualifiedName();
					ASTNode astnode = Demo.project.getClassByName(qualifiedName);
					
					if (astnode instanceof TypeDeclaration) {
						TypeDeclaration tpd = (TypeDeclaration) astnode;
						
						ObjectNode node = (ObjectNode) cg.getCGNodeByAST(tpd);
						FieldNode fieldNode = null;
						
						if (node != null) {
							fieldNode = node.getFieldNode(field);
						
							// should be GlobalEscape
							if (fieldNode != null && fieldNode.getEscapeState() == EscapeState.GlobalEscape)
								refs.add(fieldNode);
							else 
								CommonUtils.debug("field node not found!");
						}
					}
					
				}
			} else if (nodes.size() == 1) {
				// QualifierName is a variable name
				qualifierNode = handle(cg, simpleName).iterator().next();
				ptNodes = qualifierNode.getPointsToNode();
				
				if (ptNodes == null || ptNodes.isEmpty()) {
					if (CommonUtils.isParameter(simpleName) || CommonUtils.isField(simpleName)) {
						TypeDeclaration tpd = (TypeDeclaration)Demo.project.getClassByName(simpleName.resolveTypeBinding().getName());
						if (tpd != null) {
							ObjectNode phantom = cg.getOrAddJavaObjectNode(tpd);
							cg.addPointsToEdge(qualifierNode, phantom);
							ptNodes = CommonUtils.newHashSet();
							ptNodes.add(phantom);
						} else {
							CommonUtils.debug("tpd for field or parameter not found.");
						}
					} else {
						CommonUtils.debug("potential NullPointerExceptions.");
					}
				}
				
				for (ObjectNode obj : ptNodes)
					if (obj != null) {
						FieldNode fldNode = obj.getFieldNode(field);
						if (fldNode != null)
							refs.add(fldNode);
						else
							CommonUtils.debug("field node not found.");
					}
			} else
				CommonUtils.debug("SimpleName should not has more than one corresponding node.");

			
		} else if (qualifierName instanceof QualifiedName) {
			Set<CGNode> refNodes = handle(cg, (QualifiedName) qualifierName);
			if (refNodes == null) {
				// TODO java.util.ArrayList
				CommonUtils.debug("ref nodes of qulified name (" + qualifierName + ") not found.");
			} else {
				for (CGNode ref : refNodes) {
					if (ref == null) {
						CommonUtils.debug("invalid (null) ref node.");
						continue;
					}
					ptNodes = ref.getPointsToNode();
					for (ObjectNode obj : ptNodes) {
						FieldNode fldNode = obj.getFieldNode(field);
						if (fldNode != null)
							refs.add(fldNode);
						else
							CommonUtils.debug("field node not found.");
					}
				}
			}
		}

		return refs;
	}
	
	/**
	 * return the real objects that ThisExpression points to
	 * Several cases：
	 * 1. this of common classes
	 * 2. this of anonymous classes
	 * 3. ClassName.this
	 * @param dfv
	 * @param texp
	 * @return
	 */
	static Set<CGNode> handle(EncapDFV dfv, ThisExpression texp) {
		return handle(dfv.cg, texp);
	}
	
	static Set<CGNode> handle(ConnectionGraph cg, ThisExpression texp) {
		Name className = texp.getQualifier();
		Set<CGNode> ret = CommonUtils.newHashSet();
		// this
		if (className == null) {
			CGNode node = cg.getCurrThisObject();
			if (node != null)
				ret.add(node);
		}
		// ClassName.this
		else {
			TypeDeclaration enclosingTpd = getEnclosingTpd(ClassCG.currTpd, className);
			ret.add(ClassCG.tpd2obj.get(enclosingTpd));
		}
		return ret;
	}
	
	static TypeDeclaration getEnclosingTpd(TypeDeclaration tpd, Name targetClassName) {		
		String targetFullName = targetClassName.resolveTypeBinding().getQualifiedName();		
		String currentTpdName = tpd.resolveBinding().getQualifiedName();
		
		String tmp = null;
		for (tmp = currentTpdName; tmp != null && !tmp.equals(targetFullName); tmp = Demo.project.getOuterClassName(tmp))
			;
		if (tmp != null && tmp.equals(targetFullName))
			return (TypeDeclaration) Demo.project.getClassByName(tmp);

		return null;
	}

	static Set<CGNode> handle(EncapDFV dfv, FieldAccess fa) {
		return handle(dfv.cg, fa);
	}
	
	static Set<CGNode> handle(ConnectionGraph cg, FieldAccess fa) {
		SimpleName field = fa.getName();
		if (CommonUtils.isPrimitive(field) == null || CommonUtils.isPrimitive(field))
			return null;
		
		Expression efa = fa.getExpression();
		Set<CGNode> refs = CommonUtils.newHashSet();
		
		// simplify this.exp to exp
		if (efa.toString().equals("this")) {
			Set<CGNode> nodes = handle(cg, field);
			if (nodes != null)
				refs.addAll(nodes);
		} else {
			Set<CGNode> refNodes = handle(cg, efa);
			Set<ObjectNode> ptNodes = null;
			if (refNodes == null)
				return null;
			for (CGNode ref : refNodes) {
				ptNodes = ref.getPointsToNode();
				for (ObjectNode obj : ptNodes)
					refs.add(obj.getFieldNode(field));
			}
		}
		
		return refs;
	}

	static Set<CGNode> handle(EncapDFV dfv, SuperFieldAccess sfa) {
		return handle(dfv.cg, sfa);
	}
	
	static Set<CGNode> handle(ConnectionGraph cg, SuperFieldAccess sfa) {
		SimpleName fieldName = sfa.getName();
		if (CommonUtils.isPrimitive(fieldName))
			return null;
		
		Name qualifierName = sfa.getQualifier();
		Set<ObjectNode> ptNodes = null;
		Set<CGNode> refs = CommonUtils.newHashSet();
		
		Set<CGNode> refNodes = handle(cg, qualifierName);
		for (CGNode ref : refNodes) {
			ptNodes = ref.getPointsToNode();
			for (ObjectNode obj : ptNodes)
				refs.add(obj.getFieldNode(fieldName));
		}
		return refs;
	}

}
