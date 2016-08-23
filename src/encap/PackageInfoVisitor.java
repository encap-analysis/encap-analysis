package encap;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class PackageInfoVisitor extends ASTVisitor {
	
	PackageDeclaration pd = null;
	
	public PackageInfoVisitor(PackageDeclaration pd) {
		this.pd = pd;
	}
	
	public boolean visit(TypeDeclaration tpd) {
		Demo.project.connectTypeAndPackage(tpd, pd);
		
		// deal with inner classes
		return true;
	}
}
