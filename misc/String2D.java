// -*- java -*-
package eem.frame.misc;

import java.awt.geom.Point2D;

public class String2D {
	String x = "";
	String y = "";

	public String2D() {
		x="";
		y="";
	}

	public String2D(String xn, String yn) {
		x=xn;
		y=yn;
	}

	public String getX() {
		return x;
	}

	public String getY() {
		return y;
	}

	public void setLocation(String xn, String yn) {
		x=xn;
		y=yn;
	}

	public boolean equals(Object o) {
		if (o == null || !(o instanceof String2D)) return false;
		String2D other = (String2D) o;
		return x == other.x && y == other.y;
	}

	public int hashCode() {
		String sumStr = x+y;
		return sumStr.hashCode();
	}
}

