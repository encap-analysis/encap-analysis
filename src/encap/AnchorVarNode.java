package encap;

import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.SimpleName;

public class AnchorVarNode extends LocalVarNode {

	AnchorVarNode(SimpleName varName) {
		super(varName);
	}
	
	@ Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("NodeType: ").append("AnchorVarNode").append("\n");
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
		return "[AnchorVarNode: " + astnode + "]";
	}

}
