package xbrl;

import java.util.ArrayList;
import java.util.List;

public class ParaSection {

	public String text;
	public String endsWith;
	public List<ParaSection> children = new ArrayList<ParaSection>();

	public ParaSection(String text, String endsWith) {
		this.text = text;
		this.endsWith = endsWith;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		List<ParaSection> nodes = new ArrayList<ParaSection>();
		
		ParaSection p1 = new ParaSection("parent", ";");
		ParaSection p1c1 =  new ParaSection("child-1", ".");
		ParaSection p1c2 =  new ParaSection("child-2", ".");
		p1.children.add(p1c1);
		p1.children.add(p1c2);

		ParaSection p1c1c1 =  new ParaSection("child-1", ".");
		p1c1.children.add(p1c1c1);
		
		/*
	p1
		p1c1
			p1c1c1
		p1c2

		 */

	}

}
