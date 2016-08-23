package encap;

import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.SimpleName;

public class LocalVarNode extends RefNode {
	protected Set<CGNode> ptNodes = CommonUtils.newHashSet();

	LocalVarNode(SimpleName varName) {
		super(varName);
	}
	
	@ Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("NodeType: ").append("LocalVarNode").append("\n");
		sb.append("ASTNode: ").append(astnode).append("\n");
		sb.append("EscapeState: ").append(escape).append("\n");
		
		sb.append("In: \n");
		for (Entry<CGNode, EdgeType> entry : inEdges.entrySet()) {
			sb.append("\t").append(entry.getKey().toSimpleString()).append("\n");
		}
		sb.append("Out: \n");
		for (Entry<CGNode, EdgeType> entry : outEdges.entrySet()) {
			sb.append("\t").append(entry.getKey().toSimpleString()).append("\n");
		}
//		for (CGNode pt : ptNodes) {
//			sb.append("\t\t").append(pt.astnode).append("\t");
//			if (pt instanceof ObjectNode)
//				sb.append(EdgeType.PointsToEdge).append("\n");
//			else
//				sb.append(EdgeType.DeferredEdge).append("\n");
//		}
//		sb.append("---------------------------------");
		
		return sb.toString();
	}
	
	@ Override
	public String toSimpleString() {
		return "[LocalVarNode: " + astnode + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 3;
		result = prime * result + ((astnode == null) ? 0 : hashIndex);
		
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		return isEqualTo(obj);
	}

	@Override
	public boolean isEqualTo(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		LocalVarNode localVarNode = (LocalVarNode)obj;		
		return this.correspondTo(localVarNode.getASTNode());
	}

	@Override
	public NodeType getNodeType() {
		return NodeType.LocalVar;
	}

	@Override
	public Object clone() {
		LocalVarNode lvn = new LocalVarNode((SimpleName)astnode);
		lvn.setEscapeState(this.escape);
		return lvn;
	}

	@Override
	public String toNeo4jString() {
		return astnode.toString();
	}
}
