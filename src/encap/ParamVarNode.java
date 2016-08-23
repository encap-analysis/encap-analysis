package encap;

import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.SimpleName;

public class ParamVarNode extends LocalVarNode {
	ParamVarNode(SimpleName varName) {
		super(varName);
		setEscapeState(EscapeState.ArgEscape);
	}
	
	@ Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("NodeType: ").append("ParamVarNode").append("\n");
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
		
		return sb.toString();
	}
	
	public String toSimpleString() {
		return "[ParamVarNode: " + astnode + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 4;
		result = prime * result + ((astnode == null) ? 0 : hashIndex);
		
		return result;
	}

	@Override
	public NodeType getNodeType() {
		return NodeType.ParamVar;
	}
	
	@Override
	public Object clone() {
		LocalVarNode lvn = new ParamVarNode((SimpleName)astnode);
		return lvn;
	}
}