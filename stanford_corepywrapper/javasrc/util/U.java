package util;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import util.misc.Pair;
import util.misc.Triple;


import edu.stanford.nlp.util.StringUtils;

/** misc utilities & convenience wrappers **/
public class U {
	
	/** static creator so don't have to specify types **/
	public static <S,T> Pair<S,T> pair(S o1, T o2) {
		return new Pair<S,T>(o1, o2);
	}

	/** static creator so don't have to specify types **/
	public static <S,T,V> Triple<S,T,V> triple(S o1, T o2, V o3) {
		return new Triple<S,T,V>(o1, o2, o3);
	}
	
	public static final String ANSI_RESET 	= "\u001B[0m";
	public static final String ANSI_BOLD 	= "\u001B[1m";	
	public static final String ANSI_BLACK 	= "\u001B[30m";
	public static final String ANSI_RED 	= "\u001B[31m";
	public static final String ANSI_GREEN 	= "\u001B[32m";
	public static final String ANSI_YELLOW 	= "\u001B[33m";
	public static final String ANSI_BLUE 	= "\u001B[34m";
	public static final String ANSI_PURPLE 	= "\u001B[35m";
	public static final String ANSI_CYAN 	= "\u001B[36m";
	public static final String ANSI_WHITE 	= "\u001B[37m";
	public static String red(String s) { 	return ANSI_RED + s + ANSI_RESET; }
	public static String green(String s) { 	return ANSI_GREEN + s + ANSI_RESET; }
	public static String blue(String s) { 	return ANSI_BLUE + s + ANSI_RESET; }
	public static String purple(String s) { return ANSI_PURPLE + s + ANSI_RESET; }
	
	
	public static void p(Object x) { System.out.println(x); }
	public static void p(String[] x) { p(Arrays.toString(x)); }
	public static void p(double[] x) { p(Arrays.toString(x)); }
	public static void p(float[] x) { p(Arrays.toString(x)); }
	public static void p(int[] x) { p(Arrays.toString(x)); }
	public static void p(double[][] x) {
		System.out.printf("(%s x %s) [\n", x.length, x[0].length);
		for (double[] row : x) {
			System.out.printf(" ");
			p(Arrays.toString(row));
		}
		p("]");
	}
	public static void p(int[][] x) {
		System.out.printf("(%s x %s) [\n", x.length, x[0].length);
		for (int[] row : x) {
			System.out.printf(" ");
			p(Arrays.toString(row));
		}
		p("]");
	}
	public static String sp(double[] x) {
		ArrayList<String> parts = new ArrayList<String>();
		for (int i=0; i < x.length; i++)
			parts.add(String.format("%.2g", x[i]));
		return "[" + StringUtils.join(parts) + "]";
	}
	public static void p(String x) { System.out.println(x); }
	
	public static String sf(String pat, double[] a0) {  return Arr.sf(pat, a0);  }
	public static void pf(String pat) {  System.out.printf(pat);  }

	public static <A> void pf(String pat, A a0) {  System.out.printf(pat, a0);  }
	public static <A> String sf(String pat, A a0) {  return String.format(pat, a0);  }
	public static <A,B> void pf(String pat, A a0, B a1) {  System.out.printf(pat, a0, a1);  }
	public static <A,B> String sf(String pat, A a0, B a1) {  return String.format(pat, a0, a1);  }
	public static <A,B,C> void pf(String pat, A a0, B a1, C a2) {  System.out.printf(pat, a0, a1, a2);  }
	public static <A,B,C> String sf(String pat, A a0, B a1, C a2) {  return String.format(pat, a0, a1, a2);  }
	public static <A,B,C,D> void pf(String pat, A a0, B a1, C a2, D a3) {  System.out.printf(pat, a0, a1, a2, a3);  }
	public static <A,B,C,D> String sf(String pat, A a0, B a1, C a2, D a3) {  return String.format(pat, a0, a1, a2, a3);  }
	public static <A,B,C,D,E> void pf(String pat, A a0, B a1, C a2, D a3, E a4) {  System.out.printf(pat, a0, a1, a2, a3, a4);  }
	public static <A,B,C,D,E> String sf(String pat, A a0, B a1, C a2, D a3, E a4) {  return String.format(pat, a0, a1, a2, a3, a4);  }
	public static <A,B,C,D,E,F> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5);  }
	public static <A,B,C,D,E,F> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5) {  return String.format(pat, a0, a1, a2, a3, a4, a5);  }
	public static <A,B,C,D,E,F,G> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6);  }
	public static <A,B,C,D,E,F,G> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6);  }
	public static <A,B,C,D,E,F,G,H> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7);  }
	public static <A,B,C,D,E,F,G,H> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7);  }
	public static <A,B,C,D,E,F,G,H,I> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8);  }
	public static <A,B,C,D,E,F,G,H,I> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8);  }
	public static <A,B,C,D,E,F,G,H,I,J> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9);  }
	public static <A,B,C,D,E,F,G,H,I,J> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9);  }
	public static <A,B,C,D,E,F,G,H,I,J,K> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);  }
	public static <A,B,C,D,E,F,G,H,I,J,K> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14, P a15) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14, P a15) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14, P a15, Q a16) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14, P a15, Q a16) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14, P a15, Q a16, R a17) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14, P a15, Q a16, R a17) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14, P a15, Q a16, R a17, S a18) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14, P a15, Q a16, R a17, S a18) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14, P a15, Q a16, R a17, S a18, T a19) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14, P a15, Q a16, R a17, S a18, T a19) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19);  }

	public static Properties loadProperties(String filename) {
		Properties properties= new Properties();
		try {
			properties.load(new FileInputStream(filename));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return properties;
	}
	public static void setField(Object obj, String fieldName, double value) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Field f = obj.getClass().getDeclaredField(fieldName);
		f.setDouble(obj, value);
	}
	public static void setField(Object obj, String fieldName, int value) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Field f = obj.getClass().getDeclaredField(fieldName);
		f.setInt(obj, value);
	}
	public static void setFieldDouble(Object obj, Properties props, String fieldName) {
		if (! props.containsKey(fieldName)) {
			return;
		}
		double x = Double.valueOf((String) props.getProperty(fieldName));
		try {
			setField(obj, fieldName, x);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			p(fieldName + " in properties but not class; skipping.");
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	public static void setFieldInteger(Object obj, Properties props, String fieldName) {
		if (! props.containsKey(fieldName)) {
			return;
		}
		int x = Integer.valueOf((String) props.getProperty(fieldName));
		try {
			setField(obj, fieldName, x);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			p(fieldName + " in properties but not class; skipping.");
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	public static Class getNestedClass(Class klass, String name) {
		for (Class c : klass.getDeclaredClasses()) {
			if (c.getName().endsWith("$" + name)) {
				return c;
			}
		}
		return null;
	}
}
