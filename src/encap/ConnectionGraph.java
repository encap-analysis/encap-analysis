package encap;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;

public class ConnectionGraph {	
	private Set<CGNode> cgnodes = CommonUtils.newHashSet();

	public ConnectionGraph() {
		// do nothing
	}
	
	public int size() {
		return cgnodes.size();
	}
	
	public CGNode getCGNodeByAST(ASTNode ast, NodeType type) {
		if (ast == null)
			return null;
		for (CGNode node : cgnodes) {
			if (node == null) {
				CommonUtils.printlnError("Error: invalid node in CG!");
				continue;
			}
			
			if (node.correspondTo(ast) && node.getNodeType() == type)
				return node;
		}
		return null;
	}
	
	public CGNode getCGNodeByAST(ASTNode ast) {
		if (ast == null)
			return null;
		for (CGNode node : cgnodes) {
			if (node == null)
				CommonUtils.printlnError("Error: invalid node in CG!");
			
			if (node.correspondTo(ast))
				return node;
		}
		return null;
	}

	public ObjectNode getOrAddJavaObjectNode(ASTNode ast) {
		ObjectNode node = (ObjectNode)getCGNodeByAST(ast, NodeType.JavaObject);
		if (node != null)
			return node;
		
		return addJavaObjectNode(ast);
	}

	public ObjectNode addJavaObjectNode(ASTNode astnode) {
		ObjectNode objNode = null;
		if (astnode instanceof TypeDeclaration)
			objNode = new ObjectNode((TypeDeclaration)astnode);
		else if (astnode instanceof SingleVariableDeclaration)
			objNode = new ObjectNode((SingleVariableDeclaration)astnode);
		else if (astnode instanceof ClassInstanceCreation)
			objNode = new ObjectNode((ClassInstanceCreation)astnode);

		if (objNode == null)
			CommonUtils.debug("unkown ASTNode for ObjectNode (" + astnode + ")");
		else
			cgnodes.add(objNode);
		
		return objNode;
	}
	
	public FieldNode getFieldNode(ObjectNode objNode, ASTNode ast) {
		return objNode.getFieldNode((SimpleName)ast);
	}
	
	public LocalVarNode getOrAddLocalVarNode(ASTNode ast) {
		LocalVarNode node = (LocalVarNode)getCGNodeByAST(ast, NodeType.LocalVar);
		if (node != null)
			return node;

		return addLocalVarNode(ast);
	}
	
	public LocalVarNode addLocalVarNode(ASTNode ast) {
		if (!(ast instanceof SimpleName))
			return null;
		LocalVarNode node = new LocalVarNode((SimpleName)ast);
		cgnodes.add(node);
		return node;
	}
	
	public ParamVarNode getParamVarNode(ASTNode ast) {
		ParamVarNode node = (ParamVarNode)getCGNodeByAST(ast, NodeType.ParamVar);
		if (node != null)
			return node;

		CommonUtils.debug("ParamVarNode " + ast + " not found!");
		return null;
	}
	
	public ParamVarNode addParamVarNode(ASTNode ast) {
		ParamVarNode node = new ParamVarNode((SimpleName)ast);
		cgnodes.add(node);
		return node;
	}
	
	void addDeferredEdge(CGNode from, CGNode to) {
		if (from == null || to == null)
			return;
		if (!(from instanceof ObjectNode) && !(to instanceof ObjectNode)) {
			from.addOutEdge(to, EdgeType.DeferredEdge);
			to.addInEdge(from, EdgeType.DeferredEdge);
		}
	}
	
	void removeDeferredEdge(CGNode from, CGNode to) {
		if (from == null || to == null)
			return;
		if (!(from instanceof ObjectNode) && !(to instanceof ObjectNode)) {
			from.removeOutEdge(to, EdgeType.DeferredEdge);
			to.removeInEdge(from, EdgeType.DeferredEdge);
		}
	}
	
	void addPointsToEdge(CGNode from, ObjectNode to) {
		if (from == null || to == null)
			return;
		if (!(from instanceof ObjectNode)) {
			from.addOutEdge(to, EdgeType.PointsToEdge);
			to.addInEdge(from, EdgeType.PointsToEdge);
		}
	}
	
	void removePointsToEdge(CGNode from, ObjectNode to) {
		if (from == null || to == null)
			return;
		if (!(from instanceof ObjectNode)) {
			from.removeOutEdge(to, EdgeType.PointsToEdge);
			to.removeInEdge(from, EdgeType.PointsToEdge);
		}
	}
	
	void addFieldConnection(CGNode from, CGNode to, SimpleName fieldName) {		
		if (from instanceof ObjectNode && to instanceof ObjectNode) {		
			ObjectNode fromObjNode = (ObjectNode)from;
			ObjectNode toObjNode = (ObjectNode)to;
			
			FieldNode fldNode = fromObjNode.getFieldNode(fieldName);
			fromObjNode.addOutEdge(fieldName, to, EdgeType.PointsToEdge);
			toObjNode.addInEdge(fldNode, EdgeType.PointsToEdge);
		}
	}
	
	/**
	 * first, copy vertexes
	 * second, copy edges
	 */
	@ Override
	public ConnectionGraph clone() {
		ConnectionGraph cp = cloneHelperCopyNodes();
		cloneHelperConnectNodes(this, cp);
		if (!this.equals(cp))
			CommonUtils.debug("incorrect clone of ConnectionGraph.");
		return cp;
	}
	
	/**
	 * copy vertexes
	 * @return
	 */
	private ConnectionGraph cloneHelperCopyNodes() {
		ConnectionGraph cp = new ConnectionGraph();
		for (CGNode node : this.cgnodes) {
			if (node == null || (!(node instanceof LocalVarNode) && !(node instanceof ParamVarNode) && !(node instanceof ObjectNode))) {
				CommonUtils.printlnError("Invalid nodes in the connection graph!");
				continue;
			}
			CGNode newNode = (CGNode)node.clone();
			if (newNode == null)
				CommonUtils.printlnError("Clone of CG node failed!");
			cp.cgnodes.add(newNode);
		}		
		return cp;
	}
	
	/**
	 * copy edges
	 * @param source
	 * @param cp
	 * @return
	 */
	private ConnectionGraph cloneHelperConnectNodes(ConnectionGraph source, ConnectionGraph cp) {		
		for (CGNode node : source.cgnodes) {
			if (node == null)
				continue;
			CGNode node2 = cp.getCGNodeByAST(node.getASTNode(), node.getNodeType());
			if (node2 == null)
				continue;
			
			// In edges of normal nodes
			for (Entry<CGNode, EdgeType> entry : node.getInEdges().entrySet()) {
				CGNode in = entry.getKey();
				EdgeType et = entry.getValue();
				if (in instanceof LocalVarNode || in instanceof ParamVarNode) {
					CGNode in2 = cp.getCGNodeByAST(in.getASTNode(), in.getNodeType());
					if (in2 != null)
						node2.addInEdge(in2, et);
				} else if (in instanceof FieldNode) {
					FieldNode fldNode = (FieldNode)in;
					ObjectNode owner2 = (ObjectNode)cp.getCGNodeByAST(fldNode.getOwner().getASTNode(), NodeType.JavaObject);
					if (owner2 != null) {
						CGNode in2 = owner2.getFieldNode(fldNode.getSimpleName());
						node2.addInEdge(in2, et);
					}
				}
			}

			// Out edges
			if (node instanceof LocalVarNode || node instanceof ParamVarNode) {
				for (Entry<CGNode, EdgeType> entry : node.getOutEdges().entrySet()) {
					CGNode out = entry.getKey();
					EdgeType et = entry.getValue();
					if (out instanceof LocalVarNode || out instanceof ParamVarNode || out instanceof ObjectNode) {
						CGNode out2 = cp.getCGNodeByAST(out.getASTNode(), out.getNodeType());
						if (out2 != null)
							node2.addOutEdge(out2, et);
					} else if (out instanceof FieldNode) {
						FieldNode fldNode = (FieldNode)out;
						ObjectNode owner2 = (ObjectNode)cp.getCGNodeByAST(fldNode.getOwner().getASTNode(), NodeType.JavaObject);
						if (owner2 != null) {
							CGNode out2 = owner2.getFieldNode(fldNode.getSimpleName());
							node2.addOutEdge(out2, et);
						}
					}
				}
			} else if (node instanceof ObjectNode) {
				// In and out edges of FieldNode
				for (FieldNode fldNode : ((ObjectNode)node).getFieldNodes()) {
					for (Entry<CGNode, EdgeType> entry : fldNode.getInEdges().entrySet()) {
						CGNode in = entry.getKey();
						EdgeType et = entry.getValue();
						FieldNode fldNodeTo = ((ObjectNode)node2).getFieldNode(fldNode.getSimpleName());
						if (in instanceof LocalVarNode || in instanceof ParamVarNode || in instanceof ObjectNode) {
							CGNode in2 = cp.getCGNodeByAST(in.getASTNode(), in.getNodeType());
							if (in2 != null)
								fldNodeTo.addInEdge(in2, et);
						} else if (in instanceof FieldNode) {
							FieldNode fldNodeFrom = (FieldNode)in;
							ObjectNode owner2 = (ObjectNode)cp.getCGNodeByAST(fldNodeFrom.getOwner().getASTNode(), NodeType.JavaObject);
							if (owner2 != null) {
								FieldNode in2 = owner2.getFieldNode(fldNodeFrom.getSimpleName());
								fldNodeTo.addInEdge(in2, et);
							}
						}
					}
					
					for (Entry<CGNode, EdgeType> entry : fldNode.getOutEdges().entrySet()) {
						CGNode out = entry.getKey();
						EdgeType et = entry.getValue();
						FieldNode fldNodeFrom = ((ObjectNode)node2).getFieldNode(fldNode.getSimpleName());
						if (out instanceof LocalVarNode || out instanceof ParamVarNode || out instanceof ObjectNode) {
							CGNode out2 = cp.getCGNodeByAST(out.getASTNode(), out.getNodeType());
							if (out2 != null)
								fldNodeFrom.addOutEdge(out2, et);
						} else if (out instanceof FieldNode) {
							FieldNode fldNodeTo = (FieldNode)out;
							ObjectNode owner2 = (ObjectNode)cp.getCGNodeByAST(fldNodeTo.getOwner().getASTNode(), NodeType.JavaObject);
							if (owner2 != null) {
								FieldNode out2 = owner2.getFieldNode(fldNodeTo.getSimpleName());
								fldNodeFrom.addOutEdge(out2, et);
							}
						}
					}
				}
			} else {
				CommonUtils.printlnError("Probably an error!");
			}
		}
		
		return cp;		
	}

	/**
	 * merge the target CG in to current CG
	 * first, copy vertexes; second, copy edges.
	 * otherCG remain unchaged
	 * @param otherCG
	 */
	void merge(ConnectionGraph otherCG) {
		for (CGNode otherNode : otherCG.cgnodes) {
			CGNode correspondNode = getCGNodeByAST(otherNode.getASTNode(), otherNode.getNodeType());
			if (correspondNode == null)
				cgnodes.add((CGNode)otherNode.clone());
			else
				correspondNode.merge(otherNode);
		}
		cloneHelperConnectNodes(otherCG, this);
	}
	
	void merge(ConnectionGraph otherCG, EscapeState es) {
		for (CGNode otherNode : otherCG.cgnodes) {
			CGNode correspondNode = getCGNodeByAST(otherNode.getASTNode(), otherNode.getNodeType());
			if (correspondNode == null)
				cgnodes.add((CGNode)otherNode.clone());
			else if (correspondNode.getEscapeState() == es)
				correspondNode.merge(otherNode);
		}
		cloneHelperConnectNodes(otherCG, this);
	}
	
	void mergeObjectNodes(ConnectionGraph otherCG) {
		for (CGNode otherNode : otherCG.cgnodes) {
			if (otherNode instanceof ObjectNode) {
				ObjectNode correspondNode = (ObjectNode)getCGNodeByAST(otherNode.getASTNode(), otherNode.getNodeType());
				if (correspondNode == null)
					cgnodes.add((CGNode)otherNode.clone());
				else
					correspondNode.merge(otherNode);
			}
		}
		cloneHelperConnectNodes(otherCG, this);
	}
	
	public ConnectionGraph updateEscapeState() {
		updateEscapeState(EscapeState.GlobalEscape);
		updateEscapeState(EscapeState.ArgEscape);
		updateEscapeState(EscapeState.RetEscape);
//		updateEscapeState(EscapeState.NoEscape);
		
		return this;
	}
	
	void updateEscapeState(EscapeState es) {
		Deque<CGNode> worklist = CommonUtils.newArrayDeque();

		Set<CGNode> history = CommonUtils.newHashSet();
		
		for (CGNode node : this.cgnodes) {
			if (node != null && node.getEscapeState() == es) {
				worklist.push(node);
				history.add(node);
			}
		}		
		while (worklist.size() > 0) {
			CGNode node = worklist.pop();
			if (node instanceof LocalVarNode || node instanceof ParamVarNode) {
				updateEscapeStateHelper(node, worklist, es, history);
			} else if (node instanceof ObjectNode) {
				ObjectNode objNode = (ObjectNode)node;
				for (FieldNode fldNode : objNode.getFieldNodes()) {
					if (fldNode.adjustEscapeState(es))
						;
//						Utils.println("EscapeState of " + fldNode.toSimpleString() + " is changed to " + es);
					updateEscapeStateHelper(fldNode, worklist, es, history);
				}
			}
		}
	}
	
	// helper method
	private void updateEscapeStateHelper(CGNode node, Deque<CGNode> worklist, EscapeState es, Set<CGNode> history) {
		for (Entry<CGNode, EdgeType> entry : node.getOutEdges().entrySet()) {
			CGNode pt = entry.getKey();
			if (pt.adjustEscapeState(es))
				;
//				Utils.println("EscapeState of " + pt.toSimpleString() + " is changed to " + es);
			if (!history.contains(pt)) {
				worklist.push(pt);
				history.add(pt);
			}
		}
	}
	
	/**
	 * remove isolated nodes
	 * @param exception
	 */
	void removeIsolatedNodes(CGNode exception) {
		Iterator<CGNode> iter = cgnodes.iterator();
		outer: while (iter.hasNext()) {
			CGNode node = iter.next();
			if (node.correspondTo(exception.getASTNode()))
				continue;
			if (node instanceof LocalVarNode || node instanceof ParamVarNode) {
				if (node.inEdges.size() == 0 && node.outEdges.size() == 0)
					iter.remove();
			} else if (node instanceof ObjectNode) {
				if (node.inEdges.size() == 0) {
					for (FieldNode fldNode : ((ObjectNode) node).getFieldNodes()) {
						if (fldNode.inEdges.size() != 0 || fldNode.outEdges.size() != 0)
							continue outer;
					}
					iter.remove();
				}
			}
		}	
	}
	
	/**
	 * logical equality
	 */
	@ Override
	public boolean equals(Object obj)
	{
		if(!(obj instanceof ConnectionGraph))
			return false;
		
		ConnectionGraph other = (ConnectionGraph)obj;
		if (this.cgnodes.size() != other.cgnodes.size())
			return false;

		// compare CGNode one by one
		for (CGNode node : this.cgnodes) {
			CGNode node2 = other.getCGNodeByAST(node.getASTNode(), node.getNodeType());
			if (node2 == null)
				return false;
			if (!node.isEqualTo(node2))
				return false;
		}
		
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (CGNode node : cgnodes) {
			if (node != null) {
				sb.append(node.toString());
				sb.append("---------------------------------------\n");
			}
		}
		return sb.toString();
	}
	
	// map the SummaryCG of the Callee in to the CG of the Caller中
	Set<CGNode> map(MethodInvocation mti) {
		MethodDeclaration mtd = CommonUtils.findDeclarationFor(mti);
		if (mtd == null)
			return null;
		
		List<Expression> args = mti.arguments();
		List<SingleVariableDeclaration> params = mtd.parameters();
		
		MethodCG mcg = ClassCG.getMethodCG(mtd);
		if (mcg == null || mcg.getExitCG() == null)
			return null;
		ConnectionGraph calleeCG = mcg.getExitCG();
		
		Map<ASTNode, ASTNode> callee2callers = CommonUtils.newHashMap();
		
		
		Expression exp = mti.getExpression();
		
		TypeDeclaration tpd = CommonUtils.getDeclaringTpd(mtd);
		ObjectNode calleeObj = (ObjectNode) calleeCG.getCGNodeByAST(tpd, NodeType.JavaObject);
		if (calleeObj == null)
			CommonUtils.debug("calleeObj not found.");		
		
		callee2callers.put(tpd, exp);
		
		if (exp != null) {
			Set<CGNode> nodes = EncapDFVUtils.handle(this, exp);
			if (nodes != null) {
				Set<ObjectNode> callerObjs = CommonUtils.newHashSet();
				for (CGNode node : nodes) {
					if (node instanceof ObjectNode)
						callerObjs.add((ObjectNode)node);
					else if (node instanceof RefNode)
						callerObjs.addAll(node.getPointsToNode());
				}
				
				for (ObjectNode callerObj : callerObjs)
					relate(calleeObj, callerObj);
			}
		}
		else {
			ObjectNode callerObj = (ObjectNode) getCGNodeByAST(tpd, NodeType.JavaObject);
			if (callerObj != null)
				relate(calleeObj, callerObj);
		}
		
		
		

		for (int i = 0; i < params.size(); ++i) {
			SingleVariableDeclaration param = params.get(i);
			if (CommonUtils.isPrimitive(param.getName()))
				continue;
			
			if (param.isVarargs())
				continue;
			
			Expression arg = null;
			try {
				arg = args.get(i);
			} catch (java.lang.IndexOutOfBoundsException e) {
				CommonUtils.debug();
			}
			ParamVarNode paramVarNode = calleeCG.getParamVarNode(param.getName());
			
			callee2callers.put(param, arg);
			

			Set<CGNode> nodes = EncapDFVUtils.handle(this, arg);
			if (nodes != null) {				

				AnchorVarNode anchorVarNode = new AnchorVarNode(param.getName());
				cgnodes.add(anchorVarNode);
				
				for (CGNode node : nodes) {
					// refers-to edge
					if (node instanceof RefNode)
						addDeferredEdge(anchorVarNode, node);
					// points-to edge
					else if (node instanceof ObjectNode)
						addPointsToEdge(anchorVarNode, (ObjectNode)node);
				}
				if (paramVarNode != null && anchorVarNode != null)
					relate(paramVarNode, anchorVarNode);
				
				// Remove anchor nodes
				removeAnchorNodes();
			}
		}
		
		// return exp
		Set<CGNode> retNodes = CommonUtils.newHashSet();
		for (CGNode node : mcg.getRetNodes()) {
			CGNode ret = null;
			if (node instanceof LocalVarNode || node instanceof ParamVarNode || node instanceof ObjectNode) {
				ASTNode mapNode = callee2callers.get(node.getASTNode());
				if (mapNode != null)
					ret = getCGNodeByAST(mapNode);

				if (ret != null)
					retNodes.add(ret);
			}
			else if (node instanceof FieldNode) {
				ObjectNode owner = ((FieldNode) node).getOwner();	// callee TPD
				
				ASTNode mapAstNode = callee2callers.get(owner.getASTNode());
				CGNode mapNode = getCGNodeByAST(mapAstNode);	// caller
				
				if (mapNode instanceof ObjectNode) {
					ObjectNode retOwner = (ObjectNode)mapNode;
					if (retOwner != null)
						ret = retOwner.getFieldNode(((FieldNode)node).getSimpleName());

					if (ret != null)
						retNodes.add(ret);
				} else if (mapNode instanceof RefNode) {
					Set<ObjectNode> retOwners = ((RefNode)mapNode).getPointsToNode();
					for (ObjectNode retOwner : retOwners) {
						if (retOwner != null)
							ret = retOwner.getFieldNode(((FieldNode)node).getSimpleName());

						if (ret != null)
							retNodes.add(ret);
					}
				}
			}
		}
		
		return retNodes;
	}

	Set<CGNode> history = CommonUtils.newHashSet();
	
	private void relate(RefNode calleeRefNode, RefNode callerRefNode) {
		if (callerRefNode == null) {
			CommonUtils.debug("invalid caller ref node.");
			return;
		}
			
		Set<ObjectNode> calleePtNodes = calleeRefNode.getPointsToNode();
		for (ObjectNode calleeObjNode : calleePtNodes) {
			Set<ObjectNode> callerPtNodes = callerRefNode.getPointsToNode();
			if (!callerPtNodes.contains(calleeObjNode)) {
				if (calleeObjNode.getType() == ObjectType.CIC) {
					ObjectNode newObjNode = getOrAddJavaObjectNode(calleeObjNode.getASTNode());
					newObjNode.merge(calleeObjNode);
					addPointsToEdge(callerRefNode, newObjNode);
					callerPtNodes = callerRefNode.getPointsToNode();
				}
			}
			
			for (ObjectNode callerObjNode : callerPtNodes) {
				if (!history.contains(calleeObjNode)) {
					history.add(calleeObjNode);

					for (FieldNode fldNode : calleeObjNode.getFieldNodes()) {
						FieldNode callerObjField = callerObjNode.getFieldNode(fldNode.getSimpleName());
						if (fldNode != null && callerObjField != null)
							relate(fldNode, callerObjField);
					}
				}
			}
		}
	}
	
	private void relate(ObjectNode calleeObj, ObjectNode callerObj) {
		if (calleeObj == null || callerObj == null) {
			CommonUtils.debug("invalid calleeObj or callerObj.");
			return;
		}
		// calleeObj must be TPD
		if (!(calleeObj.getASTNode() instanceof TypeDeclaration))
			return;
		
		
		callerObj.merge(calleeObj);
		for (FieldNode fldNode : calleeObj.getFieldNodes()) {

			FieldNode callerObjField = callerObj.getFieldNode(fldNode.getSimpleName());
			if (callerObjField != null)
				relate(fldNode, callerObjField);
		}
	}
	
	private void removeAnchorNodes() {
		Iterator<CGNode> iter = cgnodes.iterator();
		while (iter.hasNext()) {
			CGNode node = iter.next();
			if (node instanceof AnchorVarNode) {
				Iterator<CGNode> outIter = node.outEdges.keySet().iterator();
				while (outIter.hasNext()) {
					CGNode outNode = outIter.next();
					Iterator<CGNode> inIter = node.inEdges.keySet().iterator();
					while (inIter.hasNext()) {
						CGNode inNode = inIter.next();
						if (inNode instanceof RefNode) {
							if (outNode instanceof RefNode) {
								addDeferredEdge(inNode, outNode);								
								// removeDeferredEdge(inNode, node);
								// thread-safe remove
								inNode.removeOutEdge(node, EdgeType.DeferredEdge);
								inIter.remove();
							}
							else if (outNode instanceof ObjectNode) {
								addPointsToEdge(inNode, (ObjectNode) outNode);								
								// removeDeferredEdge(inNode, node);
								// thread-safe remove
								inNode.removeOutEdge(node, EdgeType.DeferredEdge);
								inIter.remove();
							}
						}
					}
					
					if (outNode instanceof RefNode) {
						// removeDeferredEdge(node, outNode);
						// thread-safe remove
						outIter.remove();
						outNode.removeInEdge(node, EdgeType.DeferredEdge);
					}
					else if (outNode instanceof ObjectNode) {
						// removePointsToEdge(node, (ObjectNode) outNode);
						// thread-safe remove
						outIter.remove();
						outNode.removeInEdge(node, EdgeType.PointsToEdge);
					}
						
				}
				iter.remove();
			}
		}
	}
	
	public Set<CGNode> getNodes() {
		return cgnodes;
	}
	
	ObjectNode getCurrThisObject() {
		CGNode node = this.getCGNodeByAST(ClassCG.currObjNode.getASTNode(), NodeType.JavaObject);
		if (node != null)
			return (ObjectNode) node;
		return null;
	}
}

