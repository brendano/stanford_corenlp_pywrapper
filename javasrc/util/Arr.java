package util;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.math.SloppyMath;
import edu.stanford.nlp.util.CollectionUtils;
import edu.stanford.nlp.util.StringUtils;

import com.google.common.collect.Lists;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.NumberFormat;
import java.util.*;


/**
 * The array utility class to end all array utility classes.
 * Includes data manipulation, math, and I/O functions
 * on both 1-d arrays and 2-d matrixes (represented as array-of-arrays in the usual Java style).
 * 
 * Concatenates 
 *  - StanfordNLP ArrayMath 
 *  - StanfordNLP ArrayUtils
 *  - Java SDK 1.6 Arrays
 *  - some various new stuff, including matrix operations.
 *  
 * Newer vector/matrix routines are modeled after functions in R.
 * 
 * My safety convention: use "assert" to check preconditions, so only active under "-ea".
 * StanfordNLP sometimes checks and throws RuntimeExceptions.
 * 
 * TODO - this javadoc should have a structured overview of the different types of available methods
 * 
 * Since this class includes Stanford code, if this is released it has to be GPL?
 * 
 * @author brendano
 *
 */
public class Arr {
	private static final Random rand = new Random();

	//////////// new stuff  ////////////////////////
	
	public static double GROWTH_MULTIPLIER = 1.5;

	public static double[] grow(double[] x) {
		double[] y = new double[(int) Math.ceil(GROWTH_MULTIPLIER * x.length)];
		System.arraycopy(x,0, y,0, x.length);
		return y;
	}

	public static long[] grow(long[] x, double multiplier) {
		long[] y = new long[(int) Math.ceil(multiplier * x.length)];
		System.arraycopy(x,0, y,0, x.length);
		return y;
	}
	
	public static int[] grow(int[] x) {
		int[] y = new int[(int) Math.ceil(GROWTH_MULTIPLIER * x.length)];
		System.arraycopy(x,0, y,0, x.length);
		return y;
	}

	public static int[][] grow(int[][] x) {
		int[][] y = new int[(int) Math.ceil(GROWTH_MULTIPLIER * x.length)][];
		System.arraycopy(x,0, y,0, x.length);
		return y;
	}

	public static int[][][] grow(int[][][] x) {
		int[][][] y = new int[(int) Math.ceil(GROWTH_MULTIPLIER * x.length)][][];
		System.arraycopy(x,0, y,0, x.length);
		return y;
	}
	
	public static double[] abs(double[] x) {
		double[] y = new double[x.length];
		for (int i=0; i<x.length; i++)
			y[i] = Math.abs(x[i]);
		return y;
	}
	

	/** like R all(is.finite(x)) */
	public static boolean isFinite(double[] vec) {
		return !isVeryDangerous(vec);
	}
	/** like R all(is.finite(x)) */
	public static boolean isFinite(double[][] mat) {
		return !isVeryDangerous(mat);
	}
	/** like R all(is.finite(x)) */
	public static boolean isFinite(double[][][] X) {
		for (double[][] x : X) {
			if (isVeryDangerous(x)) return false;
		}
		return true;
	}
	/**
	 * Returns true if any element is a "very dangerous" double to have
	 * around, namely one that is infinite or NaN.
	 * 
	 * BTO: I hate the name of this function; not changed from StanfordNLP
	 */
	public static boolean isVeryDangerous(double[] vec) {
		for (double x : vec) if (SloppyMath.isVeryDangerous(x)) return true;
		return false;
	}
	/**
	 * Returns true if any element is a "very dangerous" double to have
	 * around, namely one that is infinite or NaN.
	 * 
	 * BTO: I hate the name of this function; not changed from StanfordNLP
	 */
	public static boolean isVeryDangerous(double[][] mat) {
		for (double[] row : mat)
			for (double x : row)
				if (SloppyMath.isVeryDangerous(x)) return true;
		return false;
	}
	/**
	 * Returns true if any element is a "very dangerous" double to have
	 * around, namely one that is infinite or NaN.
	 * 
	 * BTO: I hate the name of this function; not changed from StanfordNLP
	 */
	public static boolean isVeryDangerous(double[][][] arr) {
		for (double[][] mat : arr)
			for (double[] row : mat)
				for (double x : row)
					if (SloppyMath.isVeryDangerous(x)) return true;
		return false;
	}
	
	/** Read delimiter-separated vector of integers.  For example,
	 *   "32 4 5 65 44"
	 * with a space separator, or
	 *   "32,4,5,65,44"
	 * with a comma separator.
	 */
	public static int[] readIntVector(String payload, String sep) {
		String[] parts = payload.split(sep);
		int[] ret = new int[parts.length];
		for (int i=0; i<parts.length; i++) {
			ret[i] = Integer.valueOf(parts[i]);
		}
		return ret;
	}

	public static double[][] readDoubleMatrix(String filename) {
		int ncol = -1;
		List<Double[]> rows = Lists.newArrayList();
		for (String line : BasicFileIO.openFileLines(filename)) {
			String[] parts = line.trim().split("\\s+");
			if (ncol==-1)
				ncol = parts.length;
			Double[] row = new Double[ncol];
			assert ncol==parts.length;
			for (int j=0; j<ncol; j++)
				row[j] = Double.valueOf(parts[j]);
			rows.add(row);
		}
		double[][] mat = new double[rows.size()][ncol];
		for (int i=0; i<rows.size(); i++)
			mat[i] = Arr.toPrimitive(rows.get(i));
		return mat;
	}
	public static double[] readDoubleVector(String filename) {
		List<Double> list = Lists.newArrayList();
		for (String line : BasicFileIO.openFileLines(filename)) {
			list.add(Double.valueOf(line));
		}
		return Arr.asPrimitiveDoubleArray(list);
	}
	public static void write(double[][] arr, String filename) {
		write(arr, filename, -1);
	}
	public static void write(double[] arr, String filename) {
		write(arr, filename, -1);
	}
	/** use sigdig=-1 for no constraint. */
	public static void write(double[][] arr, String filename, int sigdig) {
		try {
			BufferedWriter w = BasicFileIO.openFileToWrite(filename);
			String fmt = sigdig>=0 ? "%." + sigdig + "g" : "%g";
			for (int i=0; i < arr.length; i++) {
				for (int j=0; j < arr[i].length; j++) {
					w.write(String.format(fmt, arr[i][j]));
					if (j < arr[i].length-1) w.write(" ");
				}
				w.write("\n");
			}
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/** use sigdig=-1 for no constraint.  recommendation: 6 */
	public static void write(double[] arr, String filename, int sigdig) {
		try {
			BufferedWriter w = BasicFileIO.openFileToWrite(filename);
			String fmt = sigdig>=0 ? "%." + sigdig + "g" : "%g";
			for (int i=0; i < arr.length; i++) {
				w.write(String.format(fmt, arr[i]));
				w.write("\n");
			}
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void write(int[][] arr, String filename) {
		try {
			BufferedWriter w = BasicFileIO.openFileToWrite(filename);
			for (int i=0; i < arr.length; i++) {
				for (int j=0; j < arr[i].length; j++) {
					w.write(Integer.toString(arr[i][j]));
					if (j < arr[i].length-1) w.write(" ");
				}
				w.write("\n");
			}
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void write(int[] arr, String filename) {
		try {
			BufferedWriter w = BasicFileIO.openFileToWrite(filename);
			for (int i=0; i < arr.length; i++) {
				w.write(Integer.toString(arr[i]));
				w.write("\n");
			}
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static double[] concat(double[] a, double[] b) {
		double[] ret = new double[a.length + b.length];
		System.arraycopy(a,0, ret,0, 		a.length);
		System.arraycopy(b,0, ret,a.length,	b.length);
		return ret;
	}
	//	public static void main(String args[]) {
	//		U.p(concat(new double[]{1,2,3}, new double[]{4,5}));
	//	}

	/**
	 * Scales the values in this matrix by c.
	 */
	public static double[][] multiply(double[][] a, double c) {
		double[][] result = new double[a.length][a[0].length];
		for (int i = 0; i < a.length; i++) {
			for (int j=0; j < a[0].length; j++) {
				result[i][j] = a[i][j] * c;
			}
		}
		return result;
	}

	/** top-and-bottom concatenation, like R rbind() */
	public static double[][] rbind(double[][] a, double [][] b) {
		int ncol = a[0].length; assert ncol==b[0].length;
		int nrow = a.length + b.length;
		double[][] ret = new double[nrow][ncol];
		for (int i=0; i<a.length; i++) {
			ret[i] = copy(a[i]);
		}
		for (int i=0; i<b.length; i++) {
			ret[i+a.length] = b[i];
		}
		return ret;
	}
	/** top-and-bottom concatenation, like R rbind() */
	public static double[][] rbind(double[][] a, double [] b) {
		double[][] bmat = new double[][]{ b };
		return rbind(a, bmat);
	}
	public static double[][] rbind(double[] a, double [][] b) {
		double[][] amat = new double[][]{ a };
		return rbind(amat, b);
	}
	//	public static void main(String args[]) {
	//		double[][] a = new double[][] { {1,2,3}, {4,5,6} };
	//		double[][] b = new double[][] { {7,8,9} };
	//		U.p(rbind(a,b));
	//		U.p(rbind(a, new double[]{10,20,30}));
	//		U.p(rbind(new double[]{10,20,30}, a));
	//	}


	/** like R rep() */
	public static double[] rep(double value, int N) {
		double[] ret = new double[N];
		Arrays.fill(ret, value);
		return ret;
	}
	public static int[] repInts(int value, int N) {
		int[] ret = new int[N];
		Arrays.fill(ret, value);
		return ret;
	}
	/** beyond R rep() */
	public static double[][] rep(double value, int nrow, int ncol) {
		double[][] ret = new double[nrow][ncol];
		Arr.fill(ret, value);
		return ret;
	}
	/** beyond R rep() .. I guess this is do.call(rbind, rlply(nrow, function() row)) */
	public static double[][] rep(double[] row, int nrow) {
		double[][] ret = new double[nrow][row.length];
		for (int i=0; i<nrow; i++) {
			ret[i] = row;
		}
		return ret;
	}
	public double[] ones(int N) {
		return rep(1, N);
	}


	/** like R diag(): construct a diagonal matrix from a vector. */
	public static double[][] diag(double[] vec) {
		int N = vec.length;
		double[][] ret = new double[N][N];
		for (int i=0; i<N; i++) ret[i][i] = vec[i];
		return ret;
	}

	/** like R diag(): extract the diagonal from a matrix. */
	public static double[] diag(double[][] mat) {
		int N = mat.length;
		double[] ret = new double[N];
		for (int i=0; i < N; i++) {
			ret[i] = mat[i][i];
		}
		return ret;
	}

	/**
	 * 3-way inner product ... is there a special name for this?
	 * diag(X)*diag(Y)*Z
	 */
	public static double innerProduct(double[] x, double[] y, double[] z) {
		assert x.length==y.length && x.length==z.length;
		double s = 0;
		for (int i=0; i < x.length; i++)
			s += x[i]*y[i]*z[i];
		return s;
	}

	/**  (N x M) * (M x 1) ==> (N)   */
	public static double[] matrixMultiply(double[][] x, double[] y) {
		assert x[0].length == y.length;
		double[] ret = new double[y.length];
		for (int i=0; i < x.length; i++) {
			for (int j=0; j < x[0].length; j++) {
				ret[i] = x[i][j] * y[j];
			}
		}
		return ret;
	}

	/** the K-1 parameterization version of softmax, where there's an extra '0' term.
	 * R^{K-1} -> Simplex(K)
	 */
	public static double[] softmax1(double[] multiLogits) {
		int K = multiLogits.length + 1;
		double[] x = new double[K];
		System.arraycopy(multiLogits, 0, x, 0, K-1);
		return softmax(x);
	}
	//	public static void main(String args[]) { 
	//		double[] x = new double[]{ 1, 2, 3};
	//		U.p(softmax(x));
	//		U.p(softmax1(x));
	//	}

	/** 
	 * S(x) : R^K -> Simplex(K)
	 * S(x) = exp(x) / sum(exp(x))
	 * 
	 * No safety checks.  Dangerous!
	 */
	public static double[] softmaxUnsafe(double[] multiLogits) {
		double[] ret = copy(multiLogits);
		softmaxInPlace(ret);
		return ret;
	}
	/** 
	 * S(x) : R^K -> Simplex(K)
	 * S(x) = exp(x) / sum(exp(x))
	 * 
	 * This version safely handles skewed values, avoiding NaN's.
	 * More expensive than the unsafe version, because requires at least two passes.
	 */
	public static double[] softmax(double[] multiLogits) {
		double[] ret = Arr.copy(multiLogits);

		// in-place safety operations.
		// (1) Scan the vector to find its range
		double max = Double.NEGATIVE_INFINITY;
		double min = Double.POSITIVE_INFINITY;
		for (double x : ret) {
			if (x > max) max=x;
			if (x < min) min=x;
		}
		// (2) shift everything so that the max is i.e. 500 (since exp(500) is still safe).
		//     raise too-low values up to the floor.
		// can skip this step if all elements are within a safe range.
		if (min<-MAX_EXP_EXPONENT || max>MAX_EXP_EXPONENT) {
			double shift = MAX_EXP_EXPONENT - max; // negative shift.
			for (int i=0; i<ret.length; i++) {
				ret[i] += shift;
				if (ret[i] < -MAX_EXP_EXPONENT)
					ret[i] = -MAX_EXP_EXPONENT;
			}
		}
		softmaxInPlace(ret);
		return ret;
	}

	/** double tolerance is around 10^300 or exp(700).  make it a little lower just in case. */
	final static double MAX_EXP_EXPONENT = 500;

	//	public static void main(String[] args) {
	//		for (double[] eta : new double[][]{
	//				{1,2,3},
	//				{1000,2000,5000},
	//				{-2000,-1000},
	//				{-2000,-1000,1000,5000}
	//		}) {
	//			double[] theta;
	//			U.p("\nETA " + toString(eta));
	//			theta = softmax(eta);
	//			U.p("safe theta "+toString(theta));
	//		}
	//	}

	/** 
	 * S(x) : R^K -> Simplex(K)
	 * S(x) = exp(x) / sum(exp(x))
	 *
	 * No safety checks.  Dangerous!
	 */
	public static void softmaxInPlace(double[] x) {
		expInPlace(x);
		normalize(x);
	}

	/** vectorized sprintf (in R sprintf() spirit), ok not really vectorized since outputs String */
	public static String sf(String fmt, double[] arr) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i=0; i < arr.length; i++) {
			sb.append(U.sf(fmt, arr[i]));
			if (i < arr.length-1) sb.append(", ");
		}
		sb.append("]");
		return sb.toString();
	}

	/** like python x[-offset:] **/
	public static double[] sliceFromEnd(double[] x, int offset) {
		return Arrays.copyOfRange(x, x.length - offset, x.length);
	}

	/** like python x[-offset:] **/
	public static int[] sliceFromEnd(int[] x, int offset) {
		return Arrays.copyOfRange(x, x.length - offset, x.length);
	}

	public static double[] subArray(double[] a, int from, int to) {
		double[] result = new double[to-from];
		System.arraycopy(a, from, result, 0, to-from);
		return result;
	}
	
	public static float[] subArray(float[] a, int from, int to) {
		float[] result = new float[to-from];
		System.arraycopy(a, from, result, 0, to-from);
		return result;
	}

	public static String[] subArray(String[] a, int from, int to) {
		String[] result = new String[to-from];
		System.arraycopy(a, from, result, 0, to-from);
		return result;
	}

	public static List<Integer> where(int[] arr, int value) {
		List<Integer> ret = Lists.newArrayList();
		for (int i=0; i < arr.length; i++) {
			if (arr[i]==value) {
				ret.add(i);
			}
		}
		return ret;
	}

	/** returns vector size n 
	 * @author brendano **/
	public static int[] sampleArrayWithoutReplacement(int sampleSize, int[] population) {
		int[] indexSample = sampleWithoutReplacement(sampleSize, population.length);
		return project(population, indexSample);
	}

	/** like stanfordnlp's sampleWithoutReplacement() except doesn't crash and names make sense.
	 * Return a sample of integers out of {0 .. (popSize-1)}
	 * @param sampleSize: little-n
	 * @param popSize: big-N
	 * @author brendano
	 */
	public static int[] sampleWithoutReplacement(int sampleSize, int popSize) {
		if (sampleSize > popSize) {
			// return a random shuffle of [0..(min(n,N)-1)]
			int n = popSize;
			return sampleWithoutReplacement(n,n);
		}
		int[] sample = new int[sampleSize];
		Arr.sampleWithoutReplacement(sample, popSize);
		return sample;
	}
	//	public static void main(String args[]) {
	//		U.p(sampleWithoutReplacement(Integer.valueOf(args[0]), Integer.valueOf(args[1])));
	//	}

	/** a += b where both same shaped matrix.  
	 * naming inconsistent with stanford "pairwiseAddInPlace" **/
	public static void addInPlace(double[][] a, double[][] b) {
		assert a.length == b.length && (a.length==0 || a[0].length==b[0].length);
		for (int i = 0; i < a.length; i++) {
			for (int j=0; j < a[0].length; j++) {
				a[i][j] += b[i][j];
			}
		}
	}
	public static void addInPlace(double[][] a, double b) {
		for (int i = 0; i < a.length; i++) {
			for (int j=0; j < a[0].length; j++) {
				a[i][j] += b;
			}
		}
	}
	

	/** Convert to a row-major vector representation. */
	public static double[] convertToVector(double[][] mat) {
		int ncol = mat[0].length;
		double[] vec = new double[mat.length * ncol];		
		for (int i=0; i < mat.length; i++) {
			for (int j=0; j < mat[i].length; j++) {
				vec[i*ncol + j] = mat[i][j];
			}
		}
		return vec;
	}

	/** Convert to a row-major vector representation. */
	public static double[] convertToVector(double[][][] mat) {
		int[] dims = new int[]{mat.length, mat[0].length, mat[0][0].length};
		double[] vec = new double[dims[0]*dims[1]*dims[2]];
		for (int i=0; i<dims[0]; i++) {
			for (int j=0; j<dims[1]; j++) {
				for (int k=0; k<dims[2]; k++) {
					vec[i*dims[1]*dims[2] + j*dims[2] + k] = mat[i][j][k];
				}
			}
		}
		return vec;
	}
	//	public static void main(String[] args) {
	//		U.p(convertToVector(new double[][][]{ {{1,2,3}, {4,5,6}},{{7,8,9},{10,11,12}} } ));
	//		U.p(convertToVector(new double[][][]{ {{1,2,3,4,5}, {6,7,8,9,10}},{{11,12,13,14,15},{16,17,18,19,20}} }));
	//	}

	/**
	 * convert from a row-major vector representation, into a standard matrix (C-style array of rows).
	 * need to tell it the number of columns. */
	public static double[][] convertToMatrix(double[] vec, int ncol) {
		assert vec.length % ncol == 0;
		int nrow = vec.length / ncol;
		double[][] mat = new double[nrow][ncol];

		for (int k=0; k < vec.length; k++) {
			mat[k/ncol][k%ncol] = vec[k];
		}

		return mat;
	}

	/** like python arr[indices] **/
	public static int[] project(int[] arr, int[] indices) {
		int[] ret = new int[indices.length];
		for (int ii=0; ii < indices.length; ii++) {
			ret[ii] = arr[indices[ii]];
		}
		return ret;
	}

	/** this might be repetitive with something in stanfordnlp */
	public static int[] flip(int[] x) {
		int[] ret = new int[x.length];
		for (int i=0; i < ret.length; i++) {
			ret[i] = x[x.length - 1 - i];
		}
		return ret;
	}

	/** this might be repetitive with something in stanfordnlp */
	public static double[] flip(double[] x) {
		double[] ret = new double[x.length];
		for (int i=0; i < ret.length; i++) {
			ret[i] = x[x.length - 1 - i];
		}
		return ret;
	}

	public static int[] getCol(int[][] matrix, int col) {
		int[] ret = new int[matrix.length];
		for (int i=0; i < matrix.length; i++) {
			ret[i] = matrix[i][col];
		}
		return ret;
	}

	public static double[] getCol(double[][] matrix, int col) {
		double[] ret = new double[matrix.length];
		for (int i=0; i < matrix.length; i++) {
			ret[i] = matrix[i][col];
		}
		return ret;
	}

	/** like python range() */
	public static int[] rangeInts(int n) {
		int[] ret = new int[n];
		for (int i=0; i < n; i++) {
			ret[i] = i;
		}
		return ret;
	}
	/** like python range() */
	public static int[] rangeInts(int start, int end) {
		return rangeInts(start, end, 1);
	}
	/** like python range() */
	public static int[] rangeInts(int start, int end, int step) {
		int n = (int) Math.ceil((end-start)*1.0 / step);
		int[] ret = new int[n];
		for (int i=0; i < n; i++) {
			ret[i] = start + step*i;
		}
		return ret;
	}
	
	/** like python range() */
	public static List<Integer> rangeIntList(int end) {
		return rangeIntList(0, end, 1);
	}
	/** like python range() */
	public static List<Integer> rangeIntList(int start, int end) {
		return rangeIntList(start, end, 1);
	}
	/** like python range() */
	public static List<Integer> rangeIntList(int start, int end, int step) {
		int n = (int) Math.ceil((end-start)*1.0 / step);
		List<Integer> ret = new ArrayList<>();
		for (int i=0; i < n; i++) {
			ret.add(start + step*i);
		}
		return ret;
	}
	
	//	public static void main(String[] args) {
	//		U.p(rangeInts(0,10,9));
	//	}

	/** like python range() */
	public static double[] rangeDoubles(int n) {
		double[] ret = new double[n];
		for (int i=0; i < n; i++) {
			ret[i] = i;
		}
		return ret;
	}


	/** a *= b **/
	public static void multiplyInPlace(double[][] a, double b) {
		for (int i = 0; i < a.length; i++) {
			for (int j=0; j < a[0].length; j++) {
				a[i][j] *= b;
			}
		}
	}
	/** a *= b, elementwise */
	public static void multiplyInPlace(double[] a, double[] b) {
		for (int i=0; i<a.length; i++) {
			a[i] *= b[i];
		}
	}
	
	//////  Fairly minor convenience converters  /////
	
	public static String[] toStringArray(List<String> x) {
		return x.toArray(new String[0]);
	}
	public static int[] toIntArray(List<Integer> x) {
		return toPrimitive(x.toArray(new Integer[0]));
	}
	public static double[] toDoubleArray(List<Double> x) {
		return toPrimitive(x.toArray(new Double[0]));
	}


	//////////// EXTERNAL from edu.stanford.nlp.math.SloppyMath   //////////////////////

	/**
	 * If a difference is bigger than this in log terms, then the sum or
	 * difference of them will just be the larger (to 12 or so decimal
	 * places for double, and 7 or 8 for float).
	 */
	static final double LOGTOLERANCE = 30.0;
	static final float LOGTOLERANCE_F = 20.0f;


	//////////// EXTERNAL from edu.stanford.nlp.math.ArrayUtils   //////////////////////


	public static byte[] gapEncode(int[] orig) {
		List<Byte> encodedList = gapEncodeList(orig);
		byte[] arr = new byte[encodedList.size()];
		int i = 0;
		for (byte b : encodedList) { arr[i++] = b; }
		return arr;
	}

	public static List<Byte> gapEncodeList(int[] orig) {
		for (int i = 1; i < orig.length; i++) {
			if (orig[i] < orig[i-1]) { 
				throw new IllegalArgumentException("Array must be sorted!"); 
			}
		}

		List<Byte> bytes = new ArrayList<Byte>();

		int index = 0;    
		int prevNum = 0;
		byte currByte = 0 << 8;

		for (int f : orig) {
			String n = (f == prevNum ? "" : Integer.toString(f-prevNum, 2));
			for (int ii = 0; ii < n.length(); ii++) {
				if (index == 8) {
					bytes.add(currByte);
					currByte = 0 << 8;
					index = 0;
				}
				currByte <<= 1;
				currByte++;
				index++;
			}

			if (index == 8) {
				bytes.add(currByte);
				currByte = 0 << 8;
				index = 0;
			}
			currByte <<= 1;
			index++;

			for (int i = 1; i < n.length(); i++) {
				if (index == 8) {
					bytes.add(currByte);
					currByte = 0 << 8;
					index = 0;
				}
				currByte <<= 1;
				if (n.charAt(i) == '1') {
					currByte++;
				}
				index++;
			}
			prevNum = f;
		}

		while (index > 0 && index < 9) {
			if (index == 8) {
				bytes.add(currByte);
				break;
			}
			currByte <<= 1;
			currByte++;
			index++;
		}

		return bytes;
	}

	public static int[] gapDecode(byte[] gapEncoded) {
		return gapDecode(gapEncoded, 0, gapEncoded.length);
	}

	public static int[] gapDecode(byte[] gapEncoded, int startIndex, int endIndex) {
		List<Integer> ints = gapDecodeList(gapEncoded, startIndex, endIndex);
		int[] arr = new int[ints.size()];
		int index = 0;
		for (int i : ints) { arr[index++] = i; }
		return arr;
	}

	public static List<Integer> gapDecodeList(byte[] gapEncoded) {
		return gapDecodeList(gapEncoded, 0, gapEncoded.length);
	}

	public static List<Integer> gapDecodeList(byte[] gapEncoded, int startIndex, int endIndex) {

		boolean gettingSize = true;
		int size = 0;
		List<Integer> ints = new ArrayList<Integer>();
		int gap = 0;
		int prevNum = 0;

		for (int i = startIndex; i < endIndex; i++) {
			byte b = gapEncoded[i];
			for (int index = 7; index >= 0; index--) {
				boolean value = ((b >> index) & 1) == 1;

				if (gettingSize) {
					if (value) { size++; }
					else {
						if (size == 0) {
							ints.add(prevNum);
						} else if (size == 1) {
							prevNum++;
							ints.add(prevNum);
							size = 0;
						} else {
							gettingSize = false;
							gap = 1;
							size--;
						}
					}
				} else {
					gap <<= 1;
					if (value) { gap++; }
					size--;
					if (size == 0) {
						prevNum += gap;
						ints.add(prevNum);
						gettingSize = true;
					}
				}
			}
		}

		return ints;
	}

	public static byte[] deltaEncode(int[] orig) {
		List<Byte> encodedList = deltaEncodeList(orig);
		byte[] arr = new byte[encodedList.size()];
		int i = 0;
		for (byte b : encodedList) { arr[i++] = b; }
		return arr;
	}

	public static List<Byte> deltaEncodeList(int[] orig) {

		for (int i = 1; i < orig.length; i++) {
			if (orig[i] < orig[i-1]) { 
				throw new IllegalArgumentException("Array must be sorted!"); 
			}
		}

		List<Byte> bytes = new ArrayList<Byte>();

		int index = 0;    
		int prevNum = 0;
		byte currByte = 0 << 8;

		for (int f : orig) {
			String n = (f == prevNum ? "" : Integer.toString(f-prevNum, 2));
			String n1 = (n.length() == 0 ? "" : Integer.toString(n.length(), 2));
			for (int ii = 0; ii < n1.length(); ii++) {
				if (index == 8) {
					bytes.add(currByte);
					currByte = 0 << 8;
					index = 0;
				}
				currByte <<= 1;
				currByte++;
				index++;
			}

			if (index == 8) {
				bytes.add(currByte);
				currByte = 0 << 8;
				index = 0;
			}
			currByte <<= 1;
			index++;

			for (int i = 1; i < n1.length(); i++) {
				if (index == 8) {
					bytes.add(currByte);
					currByte = 0 << 8;
					index = 0;
				}
				currByte <<= 1;
				if (n1.charAt(i) == '1') {
					currByte++;
				}
				index++;
			}

			for (int i = 1; i < n.length(); i++) {
				if (index == 8) {
					bytes.add(currByte);
					currByte = 0 << 8;
					index = 0;
				}
				currByte <<= 1;
				if (n.charAt(i) == '1') {
					currByte++;
				}
				index++;
			}

			prevNum = f;

		}

		while (index > 0 && index < 9) {
			if (index == 8) {
				bytes.add(currByte);
				break;
			}
			currByte <<= 1;
			currByte++;
			index++;
		}

		return bytes;
	}


	public static int[] deltaDecode(byte[] deltaEncoded) {
		return deltaDecode(deltaEncoded, 0, deltaEncoded.length);
	}

	public static int[] deltaDecode(byte[] deltaEncoded, int startIndex, int endIndex) {
		List<Integer> ints = deltaDecodeList(deltaEncoded);
		int[] arr = new int[ints.size()];
		int index = 0;
		for (int i : ints) { arr[index++] = i; }
		return arr;
	}

	public static List<Integer> deltaDecodeList(byte[] deltaEncoded) {
		return deltaDecodeList(deltaEncoded, 0, deltaEncoded.length);
	}

	public static List<Integer> deltaDecodeList(byte[] deltaEncoded, int startIndex, int endIndex) {

		boolean gettingSize1 = true;
		boolean gettingSize2 = false;
		int size1 = 0;
		List<Integer> ints = new ArrayList<Integer>();
		int gap = 0;
		int size2 = 0;
		int prevNum = 0;

		for (int i = startIndex; i < endIndex; i++) {
			byte b = deltaEncoded[i];
			for (int index = 7; index >= 0; index--) {
				boolean value = ((b >> index) & 1) == 1;

				if (gettingSize1) {
					if (value) { size1++; }
					else {
						if (size1 == 0) {
							ints.add(prevNum);
						} else if (size1 == 1) {
							prevNum++;
							ints.add(prevNum);
							size1 = 0;
						} else {
							gettingSize1 = false;
							gettingSize2 = true;
							size2 = 1;
							size1--;
						}
					}
				} else if (gettingSize2) {
					size2 <<= 1;
					if (value) { size2++; }
					size1--;
					if (size1 == 0) {
						gettingSize2 = false;
						gap = 1;
						size2--;
					}
				} else {
					gap <<= 1;
					if (value) { gap++; }
					size2--;
					if (size2 == 0) {
						prevNum += gap;
						ints.add(prevNum);
						gettingSize1 = true;
					}
				}
			}
		}

		return ints;
	}                            

	/** helper for gap encoding. */
	private static byte[] bitSetToByteArray(BitSet bitSet) {

		while (bitSet.length() % 8 != 0) { bitSet.set(bitSet.length(), true); }

		byte[] array = new byte[bitSet.length()/8];

		for (int i = 0; i < array.length; i++) {
			int offset = i * 8;

			int index = 0;
			for (int j = 0; j < 8; j++) {
				index <<= 1;        
				if (bitSet.get(offset+j)) { index++; }
			}

			array[i] = (byte)(index - 128);
		}

		return array;
	}

	/** helper for gap encoding. */
	private static BitSet byteArrayToBitSet(byte[] array) {

		BitSet bitSet = new BitSet();
		int index = 0;
		for (byte b : array) {
			int b1 = ((int)b) + 128;

			bitSet.set(index++, (b1 >> 7) % 2 == 1);
			bitSet.set(index++, (b1 >> 6) % 2 == 1);
			bitSet.set(index++, (b1 >> 5) % 2 == 1);
			bitSet.set(index++, (b1 >> 4) % 2 == 1);
			bitSet.set(index++, (b1 >> 3) % 2 == 1);
			bitSet.set(index++, (b1 >> 2) % 2 == 1);
			bitSet.set(index++, (b1 >> 1) % 2 == 1);
			bitSet.set(index++, b1 % 2 == 1);
		}

		return bitSet;
	}

	//	     for (int i = 1; i < orig.length; i++) {
	//	       if (orig[i] < orig[i-1]) { throw new RuntimeException("Array must be sorted!"); }

	//	       StringBuilder bits = new StringBuilder(); 
	//	       int prevNum = 0;
	//	       for (int f : orig) {
	//	         StringBuilder bits1 = new StringBuilder();
	//	               System.err.print(f+"\t");
	//	               String n = Integer.toString(f-prevNum, 2);
	//	               String n1 = Integer.toString(n.length(), 2);
	//	               for (int ii = 0; ii < n1.length(); ii++) {
	//	                 bits1.append("1");                
	//	               }
	//	               bits1.append("0");
	//	               bits1.append(n1.substring(1));
	//	               bits1.append(n.substring(1));
	//	               System.err.print(bits1+"\t");
	//	               bits.append(bits1);
	//	               prevNum = f;
	//	             }



	public static double[] flatten(double[][] array) {
		int size = 0;
		for (double[] a : array) {
			size += a.length;
		}
		double[] newArray = new double[size];
		int i = 0;
		for (double[] a : array) {
			for (double d : a) {
				newArray[i++] = d;
			}
		}
		return newArray;
	}

	public static double[][] to2D(double[] array, int dim1Size) {
		int dim2Size = array.length/dim1Size;
		return to2D(array, dim1Size, dim2Size);
	}

	public static double[][] to2D(double[] array, int dim1Size, int dim2Size) {
		double[][] newArray = new double[dim1Size][dim2Size];
		int k = 0;
		for (int i = 0; i < newArray.length; i++) {
			for (int j = 0; j < newArray[i].length; j++) {
				newArray[i][j] = array[k++];
			}
		}
		return newArray;
	}

	/**
	 * Removes the element at the specified index from the array, and returns
	 * a new array containing the remaining elements.  If <tt>index</tt> is
	 * invalid, returns <tt>array</tt> unchanged.
	 */
	public static double[] removeAt(double[] array, int index) {
		if (array == null) {
			return null;
		}
		if (index < 0 || index >= array.length) {
			return array;
		}

		double[] retVal = new double[array.length - 1];
		for (int i = 0; i < array.length; i++) {
			if (i < index) {
				retVal[i] = array[i];
			} else if (i > index) {
				retVal[i - 1] = array[i];
			}
		}
		return retVal;
	}

	/**
	 * Removes the element at the specified index from the array, and returns
	 * a new array containing the remaining elements.  If <tt>index</tt> is
	 * invalid, returns <tt>array</tt> unchanged.  Uses reflection to determine
	 * the type of the array and returns an array of the appropriate type.
	 */
	public static Object[] removeAt(Object[] array, int index) {
		if (array == null) {
			return null;
		}
		if (index < 0 || index >= array.length) {
			return array;
		}

		Object[] retVal = (Object[]) Array.newInstance(array[0].getClass(), array.length - 1);
		for (int i = 0; i < array.length; i++) {
			if (i < index) {
				retVal[i] = array[i];
			} else if (i > index) {
				retVal[i - 1] = array[i];
			}
		}
		return retVal;
	}

	public static String toString(int[][] a) {
		StringBuilder result = new StringBuilder("[");
		for (int i = 0; i < a.length; i++) {
			result.append(Arrays.toString(a[i]));
			if(i < a.length-1)
				result.append(',');
		}
		result.append(']');
		return result.toString();
	}

	/**
	 * Tests two int[][] arrays for having equal contents.
	 * @return true iff for each i, <code>equalContents(xs[i],ys[i])</code> is true
	 */
	public static boolean equalContents(int[][] xs, int[][] ys) {
		if(xs ==null)
			return ys == null;
		if(ys == null)
			return false;
		if(xs.length != ys.length)
			return false;
		for(int i = xs.length-1; i >= 0; i--) {
			if(! equalContents(xs[i],ys[i]))
				return false;
		}
		return true;
	}

	/**
	 * Tests two double[][] arrays for having equal contents.
	 * @return true iff for each i, <code>equals(xs[i],ys[i])</code> is true
	 */
	public static boolean equals(double[][] xs, double[][] ys) {
		if(xs == null)
			return ys == null;
		if(ys == null)
			return false;
		if(xs.length != ys.length)
			return false;
		for(int i = xs.length-1; i >= 0; i--) {
			if(!Arrays.equals(xs[i],ys[i]))
				return false;
		}
		return true;
	}


	/**
	 * tests two int[] arrays for having equal contents
	 * @return true iff xs and ys have equal length, and for each i, <code>xs[i]==ys[i]</code>
	 */
	public static boolean equalContents(int[] xs, int[] ys) {
		if(xs.length != ys.length)
			return false;
		for(int i = xs.length-1; i >= 0; i--) {
			if(xs[i] != ys[i])
				return false;
		}
		return true;
	}

	/**
	 * Tests two boolean[][] arrays for having equal contents.
	 * @return true iff for each i, <code>Arrays.equals(xs[i],ys[i])</code> is true
	 */
	@SuppressWarnings("null")
	public static boolean equals(boolean[][] xs, boolean[][] ys) {
		if(xs == null && ys != null)
			return false;
		if(ys == null)
			return false;
		if(xs.length != ys.length)
			return false;
		for(int i = xs.length-1; i >= 0; i--) {
			if(! Arrays.equals(xs[i],ys[i]))
				return false;
		}
		return true;
	}


	/** Returns true iff object o equals (not ==) some element of array a. */
	public static <T> boolean contains(T[] a, T o) {
		for (T item : a) {
			if (item.equals(o)) return true;
		}
		return false;
	}

	/** Return a set containing the same elements as the specified array.
	 */
	public static <T> Set<T> asSet(T[] a) {
		return new HashSet<T>(Arrays.asList(a));
	}

	public static void fill(int[][] d, int val) {
		for (int[] aD : d) {
			Arrays.fill(aD, val);
		}
	}

	public static void fill(int[][][] d, int val) {
		for (int[][] aD : d) {
			fill(aD, val);
		}
	}

	public static void fill(int[][][][] d, int val) {
		for (int[][][] aD : d) {
			fill(aD, val);
		}
	}
	
	public static void fill(double[][] d, double val) {
		for (double[] aD : d) {
			Arrays.fill(aD, val);
		}
	}

	public static void fill(double[][][] d, double val) {
		for (double[][] aD : d) {
			fill(aD, val);
		}
	}

	public static void fill(double[][][][] d, double val) {
		for (double[][][] aD : d) {
			fill(aD, val);
		}
	}


	public static void fill(boolean[][] d, boolean val) {
		for (boolean[] aD : d) {
			Arrays.fill(aD, val);
		}
	}

	public static void fill(boolean[][][] d, boolean val) {
		for (boolean[][] aD : d) {
			fill(aD, val);
		}
	}

	public static void fill(boolean[][][][] d, boolean val) {
		for (boolean[][][] aD : d) {
			fill(aD, val);
		}
	}



	/**
	 * Casts to a double array
	 */
	 public static double[] toDouble(float[] a) {
		 double[] d = new double[a.length];
		 for (int i = 0; i < a.length; i++) {
			 d[i] = a[i];
		 }
		 return d;
	 }

	 /**
	  * Casts to a double array.
	  */
	 public static double[] toDouble(int[] array) {
		 double[] rv = new double[array.length];
		 for (int i = 0; i < array.length; i++) {
			 rv[i] = array[i];
		 }
		 return rv;
	 }

	 /** needed because Arrays.asList() won't to autoboxing,
	  * so if you give it a primitive array you get a
	  * singleton list back with just that array as an element.
	  */
	 public static List<Integer> asList(int[] array) {
		 List<Integer> l = new ArrayList<Integer>();
		 for (int i : array) {
			 l.add(i);
		 }
		 return l;
	 }


	 public static double[] asPrimitiveDoubleArray(Collection<Double> d) {
		 double[] newD = new double[d.size()];
		 int i = 0;
		 for (Double j : d) {
			 newD[i++] = j;
		 }
		 return newD;
	 }


	 public static int[] asPrimitiveIntArray(Collection<Integer> d) {
		 int[] newI = new int[d.size()];
		 int i = 0;
		 for (Integer j : d) {
			 newI[i++] = j;
		 }
		 return newI;
	 }


	 public static int[] copy(int[] i) {
		 if (i == null) { return null; }
		 int[] newI = new int[i.length];
		 System.arraycopy(i, 0, newI, 0, i.length);
		 return newI;
	 }

	 public static int[][] copy(int[][] i) {
		 if (i == null) { return null; }
		 int[][] newI = new int[i.length][];
		 for (int j = 0; j < newI.length; j++) {
			 newI[j] = copy(i[j]);
		 }
		 return newI;
	 }


	 public static double[] copy(double[] d) {
		 if (d == null) { return null; }
		 double[] newD = new double[d.length];
		 System.arraycopy(d, 0, newD, 0, d.length);
		 return newD;
	 }

	 public static double[][] copy(double[][] d) {
		 if (d == null) { return null; }
		 double[][] newD = new double[d.length][];
		 for (int i = 0; i < newD.length; i++) {
			 newD[i] = copy(d[i]);
		 }
		 return newD;
	 }

	 public static double[][][] copy(double[][][] d) {
		 if (d == null) { return null; }
		 double[][][] newD = new double[d.length][][];
		 for (int i = 0; i < newD.length; i++) {
			 newD[i] = copy(d[i]);
		 }
		 return newD;
	 }

	 public static float[] copy(float[] d) {
		 if (d == null) { return null; }
		 float[] newD = new float[d.length];
		 System.arraycopy(d, 0, newD, 0, d.length);
		 return newD;
	 }

	 public static float[][] copy(float[][] d) {
		 if (d == null) { return null; }
		 float[][] newD = new float[d.length][];
		 for (int i = 0; i < newD.length; i++) {
			 newD[i] = copy(d[i]);
		 }
		 return newD;
	 }

	 public static float[][][] copy(float[][][] d) {
		 if (d == null) { return null; }
		 float[][][] newD = new float[d.length][][];
		 for (int i = 0; i < newD.length; i++) {
			 newD[i] = copy(d[i]);
		 }
		 return newD;
	 }


	 public static String toString(boolean[][] b) {
		 StringBuilder result = new StringBuilder("[");
		 for (int i = 0; i < b.length; i++) {
			 result.append(Arrays.toString(b[i]));
			 if(i < b.length-1)
				 result.append(',');
		 }
		 result.append(']');
		 return result.toString();
	 }

	 public static long[] toPrimitive(Long[] in) {
		 return toPrimitive(in,0L);
	 }

	 public static int[] toPrimitive(Integer[] in) {
		 return toPrimitive(in,0);
	 }

	 public static short[] toPrimitive(Short[] in) {
		 return toPrimitive(in,(short)0);
	 }

	 public static char[] toPrimitive(Character[] in) {
		 return toPrimitive(in,(char)0);
	 }

	 public static double[] toPrimitive(Double[] in) {
		 return toPrimitive(in,0.0);
	 }

	 public static long[] toPrimitive(Long[] in, long valueForNull) {
		 if (in == null)
			 return null;
		 final long[] out = new long[in.length];
		 for (int i = 0; i < in.length; i++) {
			 Long b = in[i];
			 out[i] = (b == null ? valueForNull : b);
		 }
		 return out;
	 }

	 public static int[] toPrimitive(Integer[] in, int valueForNull) {
		 if (in == null)
			 return null;
		 final int[] out = new int[in.length];
		 for (int i = 0; i < in.length; i++) {
			 Integer b = in[i];
			 out[i] = (b == null ? valueForNull : b);
		 }
		 return out;
	 }

	 public static short[] toPrimitive(Short[] in, short valueForNull) {
		 if (in == null)
			 return null;
		 final short[] out = new short[in.length];
		 for (int i = 0; i < in.length; i++) {
			 Short b = in[i];
			 out[i] = (b == null ? valueForNull : b);
		 }
		 return out;
	 }

	 public static char[] toPrimitive(Character[] in, char valueForNull) {
		 if (in == null)
			 return null;
		 final char[] out = new char[in.length];
		 for (int i = 0; i < in.length; i++) {
			 Character b = in[i];
			 out[i] = (b == null ? valueForNull : b);
		 }
		 return out;
	 }

	 public static double[] toPrimitive(Double[] in, double valueForNull) {
		 if (in == null)
			 return null;
		 final double[] out = new double[in.length];
		 for (int i = 0; i < in.length; i++) {
			 Double b = in[i];
			 out[i] = (b == null ? valueForNull : b);
		 }
		 return out;
	 }

	 /**
	  * Provides a consistent ordering over arrays. First compares by the
	  * first element. If that element is equal, the next element is
	  * considered, and so on. This is the array version of
	  * {@link edu.stanford.nlp.util.CollectionUtils#compareLists} 
	  * and uses the same logic when the arrays are of different lengths.
	  */
	 public static <T extends Comparable<T>> int compareArrays(T[] first, T[] second) {
		 List<T> firstAsList = Arrays.asList(first);
		 List<T> secondAsList = Arrays.asList(second);
		 return CollectionUtils.compareLists(firstAsList, secondAsList);
	 }


	 //////////// EXTERNAL from edu.stanford.nlp.math.ArrayMath   //////////////////


	 // BASIC INFO -----------------------------------------------------------------

	 public static int numRows(double[] v) {
		 return v.length;
	 }


	 // CASTS ----------------------------------------------------------------------

	 public static float[] doubleArrayToFloatArray(double[] a) {
		 float[] result = new float[a.length];
		 for (int i = 0; i < a.length; i++) {
			 result[i] = (float) a[i];
		 }
		 return result;
	 }

	 public static double[] floatArrayToDoubleArray(float[] a) {
		 double[] result = new double[a.length];
		 for (int i = 0; i < a.length; i++) {
			 result[i] = a[i];
		 }
		 return result;
	 }

	 public static double[][] floatArrayToDoubleArray(float[][] a) {
		 double[][] result = new double[a.length][];
		 for (int i = 0; i < a.length; i++) {
			 result[i] = new double[a[i].length];
			 for (int j = 0; j < a[i].length; j++) {
				 result[i][j] = a[i][j];
			 }
		 }
		 return result;
	 }

	 public static float[][] doubleArrayToFloatArray(double[][] a) {
		 float[][] result = new float[a.length][];
		 for (int i = 0; i < a.length; i++) {
			 result[i] = new float[a[i].length];
			 for (int j = 0; j < a[i].length; j++) {
				 result[i][j] = (float) a[i][j];
			 }
		 }
		 return result;
	 }

	 public static int makeIntFromByte4(byte[] b, int offset) {
		 return (b[offset+3]&0xff)<<24 | (b[offset+2]&0xff)<<16 | (b[offset+1]&0xff)<<8 | (b[offset]&0xff);
	 }

	 public static int makeIntFromByte2(byte[] b, int offset) {
		 return (b[offset+1]&0xff)<<8 | (b[offset]&0xff);
	 }

	 // OPERATIONS ON AN ARRAY - NONDESTRUCTIVE

	 public static double[] exp(double[] a) {
		 double[] result = new double[a.length];
		 for (int i = 0; i < a.length; i++) {
			 result[i] = Math.exp(a[i]);
		 }
		 return result;
	 }
	 
	 public static double[] log(double[] a) {
		 double[] result = new double[a.length];
		 for (int i = 0; i < a.length; i++) {
			 result[i] = Math.log(a[i]);
		 }
		 return result;
	 }

	 // OPERATIONS ON AN ARRAY - DESTRUCTIVE

	 public static void expInPlace(double[] a) {
		 for (int i = 0; i < a.length; i++) {
			 a[i] = Math.exp(a[i]);
		 }
	 }

	 public static void logInPlace(double[] a) {
		 for (int i = 0; i < a.length; i++) {
			 a[i] = Math.log(a[i]);
		 }
	 }

	 // operations on matrixes -- added by BTO
	 
	 /** added by BTO */
	 public static double[][] exp(double[][] a) {
		 if (a.length==0) return new double[0][0];
		 double[][] result = new double[a.length][a[0].length];
		 for (int i = 0; i < a.length; i++) {
			 for (int j=0; j < a[0].length; j++) {
				 result[i][j] = Math.exp(a[i][j]);
			 }
		 }
		 return result;
	 }
	
	 /** added by BTO */
	 public static double[][] log(double[][] a) {
		 if (a.length==0) return new double[0][0];
		 double[][] result = new double[a.length][a[0].length];
		 for (int i = 0; i < a.length; i++) {
			 for (int j=0; j < a[0].length; j++) {
				 result[i][j] = Math.log(a[i][j]);
			 }
		 }
		 return result;
	 }
	 
	 /** added by BTO */
	 public static void expInPlace(double[][] a) {
		 for (int i = 0; i < a.length; i++) {
			 for (int j=0; j < a[i].length; j++) {
				 a[i][j] = Math.exp(a[i][j]);
			 }
		 }
	 }
	
	 /** added by BTO */
	 public static void logInPlace(double[][] a) {
		 for (int i = 0; i < a.length; i++) {
			 for (int j=0; j < a[i].length; j++) {
				 a[i][j] = Math.log(a[i][j]);
			 }
		 }
	 }
	 
	 // OPERATIONS WITH SCALAR - DESTRUCTIVE

	 /**
	  * Increases the values in this array by b. Does it in place.
	  *
	  * @param a The array
	  * @param b The amount by which to increase each item
	  */
	 public static void addInPlace(double[] a, double b) {
		 for (int i = 0; i < a.length; i++) {
			 a[i] = a[i] + b;
		 }
	 }

	 /**
	  * Increases the values in this array by b. Does it in place.
	  *
	  * @param a The array
	  * @param b The amount by which to increase each item
	  */
	 public static void addInPlace(float[] a, double b) {
		 for (int i = 0; i < a.length; i++) {
			 a[i] = (float) (a[i] + b);
		 }
	 }

	 public static void addInPlace(int[] a, int b) {
		 for (int i = 0; i < a.length; i++) {
			 a[i] += b;
		 }
	 }

	 /**
	  * Add c times the array b to array a. Does it in place.
	  */
	 public static void addMultInPlace(double[] a, double[] b, double c) {
		 for (int i=0; i<a.length; i++) {
			 a[i] += b[i] * c;
		 }
	 }

	 /**
	  * Scales the values in this array by b. Does it in place.
	  */
	 public static void multiplyInPlace(double[] a, double b) {
		 for (int i = 0; i < a.length; i++) {
			 a[i] = a[i] * b;
		 }
	 }

	 /**
	  * Scales the values in this array by b. Does it in place.
	  */
	 public static void multiplyInPlace(float[] a, double b) {
		 for (int i = 0; i < a.length; i++) {
			 a[i] = (float) (a[i] * b);
		 }
	 }

	 /**
	  * Scales the values in this array by c.
	  */
	 public static void powInPlace(double[] a, double c) {
		 for (int i = 0; i < a.length; i++) {
			 a[i] = Math.pow(a[i], c);
		 }
	 }

	 /**
	  * Sets the values in this array by to their value taken to cth power.
	  */
	 public static void powInPlace(float[] a, float c) {
		 for (int i = 0; i < a.length; i++) {
			 a[i] = (float) Math.pow(a[i], c);
		 }
	 }

	 // OPERATIONS WITH SCALAR - NONDESTRUCTIVE

	 public static double[] add(double[] a, double c) {
		 double[] result = new double[a.length];
		 for (int i = 0; i < a.length; i++) {
			 result[i] = a[i] + c;
		 }
		 return result;
	 }

	 public static float[] add(float[] a, double c) {
		 float[] result = new float[a.length];
		 for (int i = 0; i < a.length; i++) {
			 result[i] = (float) (a[i] + c);
		 }
		 return result;
	 }

	 public static int[] add(int[] a, int c) {
		 int[] result = new int[a.length];
		 for (int i = 0; i < a.length; i++) {
			 result[i] = a[i] + c;
		 }
		 return result;
	 }

	 /**
	  * Scales the values in this array by c.
	  */
	 public static double[] multiply(double[] a, double c) {
		 double[] result = new double[a.length];
		 for (int i = 0; i < a.length; i++) {
			 result[i] = a[i] * c;
		 }
		 return result;
	 }

	 /**
	  * Scales the values in this array by c.
	  */
	 public static float[] multiply(float[] a, float c) {
		 float[] result = new float[a.length];
		 for (int i = 0; i < a.length; i++) {
			 result[i] = a[i] * c;
		 }
		 return result;
	 }

	 /**
	  * raises each entry in array a by power c
	  */
	 public static double[] pow(double[] a, double c) {
		 double[] result = new double[a.length];
		 for (int i = 0; i < a.length; i++) {
			 result[i] = Math.pow(a[i], c);
		 }
		 return result;
	 }

	 /**
	  * raises each entry in array a by power c
	  */
	 public static float[] pow(float[] a, float c) {
		 float[] result = new float[a.length];
		 for (int i = 0; i < a.length; i++) {
			 result[i] = (float) Math.pow(a[i], c);
		 }
		 return result;
	 }

	 // OPERATIONS WITH TWO ARRAYS - DESTRUCTIVE

	 public static void pairwiseAddInPlace(double[] to, double[] from) {
		 if (to.length != from.length) {
			 throw new RuntimeException();
		 }
		 for (int i = 0; i < to.length; i++) {
			 to[i] = to[i] + from[i];
		 }
	 }

	 public static void pairwiseAddInPlace(double[] to, int[] from) {
		 if (to.length != from.length) {
			 throw new RuntimeException();
		 }
		 for (int i = 0; i < to.length; i++) {
			 to[i] = to[i] + from[i];
		 }
	 }

	 public static void pairwiseAddInPlace(double[] to, short[] from) {
		 if (to.length != from.length) {
			 throw new RuntimeException();
		 }
		 for (int i = 0; i < to.length; i++) {
			 to[i] = to[i] + from[i];
		 }
	 }

	 public static void pairwiseSubtractInPlace(double[] to, double[] from) {
		 if (to.length != from.length) {
			 throw new RuntimeException();
		 }
		 for (int i = 0; i < to.length; i++) {
			 to[i] = to[i] - from[i];
		 }
	 }

	 public static void pairwiseScaleAddInPlace(double[] to, double[] from, double fromScale) {
		 if (to.length != from.length) {
			 throw new RuntimeException();
		 }
		 for (int i = 0; i < to.length; i++) {
			 to[i] = to[i] + fromScale * from[i];
		 }
	 }

	 // OPERATIONS WITH TWO ARRAYS - NONDESTRUCTIVE

	 // BTO: TODO maybe drop "pairwise" from the signatures.

	 public static int[] pairwiseAdd(int[] a, int[] b) {
		 int[] result = new int[a.length];
		 for (int i = 0; i < a.length; i++) {
			 result[i] = a[i] + b[i];
		 }
		 return result;
	 }

	 public static double[] pairwiseAdd(double[] a, double[] b) {
		 double[] result = new double[a.length];
		 for (int i = 0; i < a.length; i++) {
			 if (i < b.length) {
				 result[i] = a[i] + b[i];
			 } else {
				 result[i] = a[i];
			 }
		 }
		 return result;
	 }

	 public static float[] pairwiseAdd(float[] a, float[] b) {
		 float[] result = new float[a.length];
		 for (int i = 0; i < a.length; i++) {
			 result[i] = a[i] + b[i];
		 }
		 return result;
	 }

	 public static double[] pairwiseScaleAdd(double[] a, double[] b, double bScale) {
		 double[] result = new double[a.length];
		 for (int i = 0; i < a.length; i++) {
			 result[i] = a[i] + bScale * b[i];
		 }
		 return result;
	 }


	 public static double[] pairwiseSubtract(double[] a, double[] b) {
		 double[] c = new double[a.length];

		 for (int i = 0; i < a.length; i++) {
			 c[i] = a[i] - b[i];
		 }
		 return c;
	 }

	 public static float[] pairwiseSubtract(float[] a, float[] b) {
		 float[] c = new float[a.length];

		 for (int i = 0; i < a.length; i++) {
			 c[i] = a[i] - b[i];
		 }
		 return c;
	 }

	 /**
	  * Assumes that both arrays have same length.
	  */
	  public static double[] pairwiseMultiply(double[] a, double[] b) {
		  if (a.length != b.length) {
			  throw new RuntimeException("Can't pairwise multiple different lengths: a.length=" + a.length + " b.length=" + b.length);
		  }
		  double[] result = new double[a.length];
		  for (int i = 0; i < result.length; i++) {
			  result[i] = a[i] * b[i];
		  }
		  return result;
	  }

	  /**
	   * Assumes that both arrays have same length.
	   */
	  public static float[] pairwiseMultiply(float[] a, float[] b) {
		  if (a.length != b.length) {
			  throw new RuntimeException();
		  }
		  float[] result = new float[a.length];
		  for (int i = 0; i < result.length; i++) {
			  result[i] = a[i] * b[i];
		  }
		  return result;
	  }

	  /**
	   * Puts the result in the result array.
	   * Assumes that all arrays have same length.
	   */
	  public static void pairwiseMultiply(double[] a, double[] b, double[] result) {
		  if (a.length != b.length) {
			  throw new RuntimeException();
		  }
		  for (int i = 0; i < result.length; i++) {
			  result[i] = a[i] * b[i];
		  }
	  }

	  /**
	   * Puts the result in the result array.
	   * Assumes that all arrays have same length.
	   */
	  public static void pairwiseMultiply(float[] a, float[] b, float[] result) {
		  if (a.length != b.length) {
			  throw new RuntimeException();
		  }
		  for (int i = 0; i < result.length; i++) {
			  result[i] = a[i] * b[i];
		  }
	  }

	  // ERROR CHECKING

	  public static boolean hasNaN(double[] a) {
		  for (double x : a) {
			  if (Double.isNaN(x)) return true;
		  }
		  return false;
	  }

	  public static boolean hasInfinite(double[] a) {
		  for (int i = 0; i < a.length; i++) {
			  if (Double.isInfinite(a[i])) return true;
		  }
		  return false;
	  }

	  public static boolean hasNaN(float[] a) {
		  for (float x : a) {
			  if (Float.isNaN(x)) return true;
		  }
		  return false;
	  }

	  // methods for filtering vectors ------------------------------------------

	  public static int countNaN(double[] v) {
		  int c = 0;
		  for (double d : v) {
			  if (Double.isNaN(d)) {
				  c++;
			  }
		  }
		  return c;
	  }

	  public static double[] filterNaN(double[] v) {
		  double[] u = new double[numRows(v) - countNaN(v)];
		  int j = 0;
		  for (double d : v) {
			  if ( ! Double.isNaN(d)) {
				  u[j++] = d;
			  }
		  }
		  return u;
	  }

	  public static int countInfinite(double[] v) {
		  int c = 0;
		  for (int i = 0; i < v.length; i++)
			  if (Double.isInfinite(v[i]))
				  c++;
		  return c;
	  }

	  public static int countNonZero(double[] v) {
		  int c = 0;
		  for (int i = 0; i < v.length; i++)
			  if (v[i] != 0.0)
				  ++c;
		  return c;
	  }

	  public static int countCloseToZero(double[] v, double epsilon) {
		  int c = 0;
		  for (int i = 0; i < v.length; i++)
			  if (Math.abs(v[i])< epsilon)
				  ++c;
		  return c;
	  }

	  public static int countPositive(double[] v) {
		  int c = 0;
		  for (int i = 0; i < v.length; i++)
			  if (v[i] > 0.0)
				  ++c;
		  return c;
	  }

	  public static int countNegative(double[] v) {
		  int c = 0;
		  for (int i = 0; i < v.length; i++)
			  if (v[i] < 0.0)
				  ++c;
		  return c;
	  }

	  public static double[] filterInfinite(double[] v) {
		  double[] u = new double[numRows(v) - countInfinite(v)];
		  int j = 0;
		  for (int i = 0; i < v.length; i++) {
			  if (!Double.isInfinite(v[i])) {
				  u[j++] = v[i];
			  }
		  }
		  return u;
	  }

	  public static double[] filterNaNAndInfinite(double[] v) {
		  return filterInfinite(filterNaN(v));
	  }


	  // VECTOR PROPERTIES

	  /**
	   * Returns the sum of an array of numbers.
	   */
	  public static double sum(double[] a) {
		  return sum(a,0,a.length);
	  }

	  /**
	   * Returns the sum of the portion of an array of numbers between
	   * <code>fromIndex</code>, inclusive, and <code>toIndex</code>, exclusive.
	   * Returns 0 if <code>fromIndex</code> &gt;= <code>toIndex</code>.
	   */
	  public static double sum(double[] a, int fromIndex, int toIndex) {
		  double result = 0.0;
		  for (int i = fromIndex; i < toIndex; i++) {
			  result += a[i];
		  }
		  return result;
	  }



	  public static int sum(int[] a) {
		  int result = 0;
		  for (int i : a) {
			  result += i;
		  }
		  return result;
	  }

	  public static float sum(float[] a) {
		  float result = 0.0F;
		  for (float f : a) {
			  result += f;
		  }
		  return result;
	  }

	  public static int sum(int[][] a) {
		  int result = 0;
		  for (int i = 0; i < a.length; i++) {
			  for (int j=0; j<a[i].length; j++) {
				  result += a[i][j];
			  }
		  }
		  return result;
	  }
	  
	  /** added by BTO */
	  public static double sum(double[][] a) {
		  double result = 0;
		  for (int i = 0; i < a.length; i++) {
			  for (int j=0; j<a[i].length; j++) {
				  result += a[i][j];
			  }
		  }
		  return result;
	  }

	  /**
	   * Returns diagonal elements of the given (square) matrix.
	   */
	  public static int[] diag(int[][] a) {
		  int[] rv = new int[a.length];
		  for (int i = 0; i < a.length; i++) {
			  rv[i] = a[i][i];
		  }
		  return rv;
	  }

	  /** BTO: why does this exist when mean() also exists? consider deleting. */
	  public static double average(double[] a) {
		  double total = sum(a);
		  return total / a.length;
	  }

	  /**
	   * Computes inf-norm of vector
	   *
	   * @param a Array of double
	   * @return inf-norm of a
	   */
	  public static double norm_inf(double[] a) {
		  double max = Double.NEGATIVE_INFINITY;
		  for (double d : a) {
			  if (Math.abs(d) > max) {
				  max = Math.abs(d);
			  }
		  }
		  return max;
	  }


	  /**
	   * Computes inf-norm of vector
	   *
	   * @return inf-norm of a
	   */
	  public static double norm_inf(float[] a) {
		  double max = Double.NEGATIVE_INFINITY;
		  for (int i = 0; i < a.length; i++) {
			  if (Math.abs(a[i]) > max) {
				  max = Math.abs(a[i]);
			  }
		  }
		  return max;
	  }

	  /**
	   * Computes 1-norm of vector
	   *
	   * @param a A vector of double
	   * @return 1-norm of a
	   */
	  public static double norm_1(double[] a) {
		  double sum = 0;
		  for (double anA : a) {
			  sum += (anA < 0 ? -anA : anA);
		  }
		  return sum;
	  }

	  /**
	   * Computes 1-norm of vector
	   *
	   * @param a A vector of floats
	   * @return 1-norm of a
	   */
	  public static double norm_1(float[] a) {
		  double sum = 0;
		  for (float anA : a) {
			  sum += (anA < 0 ? -anA : anA);
		  }
		  return sum;
	  }


	  /**
	   * Computes 2-norm of vector
	   *
	   * @param a A vector of double
	   * @return Euclidean norm of a
	   */
	  public static double norm(double[] a) {
		  double squaredSum = 0;
		  for (double anA : a) {
			  squaredSum += anA * anA;
		  }
		  return Math.sqrt(squaredSum);
	  }

	  /**
	   * Computes 2-norm of vector
	   *
	   * @param a A vector of floats
	   * @return Euclidean norm of a
	   */
	  public static double norm(float[] a) {
		  double squaredSum = 0;
		  for (float anA : a) {
			  squaredSum += anA * anA;
		  }
		  return Math.sqrt(squaredSum);
	  }

	  /**
	   * @return the index of the max value; if max is a tie, returns the first one.
	   */
	  public static int argmax(double[] a) {
		  double max = Double.NEGATIVE_INFINITY;
		  int argmax = 0;
		  for (int i = 0; i < a.length; i++) {
			  if (a[i] > max) {
				  max = a[i];
				  argmax = i;
			  }
		  }
		  return argmax;
	  }

	  /**
	   * @return the index of the max value; if max is a tie, returns the last one.
	   */
	  public static int argmax_tieLast(double[] a) {
		  double max = Double.NEGATIVE_INFINITY;
		  int argmax = 0;
		  for (int i = 0; i < a.length; i++) {
			  if (a[i] >= max) {
				  max = a[i];
				  argmax = i;
			  }
		  }
		  return argmax;
	  }

	  public static double max(double[] a) {
		  return a[argmax(a)];
	  }

	  public static double max(Collection<Double> a) {
		  double max = Double.NEGATIVE_INFINITY;
		  for (double d : a) {
			  if (d > max) { max = d; }
		  }
		  return max;
	  }

	  /**
	   * @return the index of the max value; if max is a tie, returns the first one.
	   */
	  public static int argmax(float[] a) {
		  float max = Float.NEGATIVE_INFINITY;
		  int argmax = 0;
		  for (int i = 0; i < a.length; i++) {
			  if (a[i] > max) {
				  max = a[i];
				  argmax = i;
			  }
		  }
		  return argmax;
	  }

	  public static float max(float[] a) {
		  return a[argmax(a)];
	  }

	  /**
	   * @return the index of the max value; if max is a tie, returns the first one.
	   */
	  public static int argmin(double[] a) {
		  double min = Double.POSITIVE_INFINITY;
		  int argmin = 0;
		  for (int i = 0; i < a.length; i++) {
			  if (a[i] < min) {
				  min = a[i];
				  argmin = i;
			  }
		  }
		  return argmin;
	  }

	  public static double min(double[] a) {
		  return a[argmin(a)];
	  }

	  /**
	   * Returns the largest value in a vector of doubles.  Any values which
	   * are NaN or infinite are ignored.  If the vector is empty, 0.0 is
	   * returned.
	   */
	  public static double safeMin(double[] v) {
		  double[] u = filterNaNAndInfinite(v);
		  if (numRows(u) == 0) return 0.0;
		  return min(u);
	  }

	  /**
	   * @return the index of the max value; if max is a tie, returns the first one.
	   */
	  public static int argmin(float[] a) {
		  float min = Float.POSITIVE_INFINITY;
		  int argmin = 0;
		  for (int i = 0; i < a.length; i++) {
			  if (a[i] < min) {
				  min = a[i];
				  argmin = i;
			  }
		  }
		  return argmin;
	  }

	  public static float min(float[] a) {
		  return a[argmin(a)];
	  }

	  /**
	   * @return the index of the max value; if max is a tie, returns the first one.
	   */
	  public static int argmin(int[] a) {
		  int min = Integer.MAX_VALUE;
		  int argmin = 0;
		  for (int i = 0; i < a.length; i++) {
			  if (a[i] < min) {
				  min = a[i];
				  argmin = i;
			  }
		  }
		  return argmin;
	  }

	  public static int min(int[] a) {
		  return a[argmin(a)];
	  }

	  /**
	   * @return the index of the max value; if max is a tie, returns the first one.
	   */
	  public static int argmax(int[] a) {
		  int max = Integer.MIN_VALUE;
		  int argmax = 0;
		  for (int i = 0; i < a.length; i++) {
			  if (a[i] > max) {
				  max = a[i];
				  argmax = i;
			  }
		  }
		  return argmax;
	  }

	  public static int max(int[] a) {
		  return a[argmax(a)];
	  }

	  /** Returns the smallest element of the matrix */
	  public static int min(int[][] matrix) {
		  int min = Integer.MAX_VALUE;
		  for (int[] row : matrix) {
			  for (int elem : row) {
				  min = Math.min(min, elem);
			  }
		  }
		  return min;
	  }

	  /** Returns the smallest element of the matrix */
	  public static int max(int[][] matrix) {
		  int max = Integer.MIN_VALUE;
		  for (int[] row : matrix) {
			  for (int elem : row) {
				  max = Math.max(max, elem);
			  }
		  }
		  return max;
	  }

	  /**
	   * Returns the largest value in a vector of doubles.  Any values which
	   * are NaN or infinite are ignored.  If the vector is empty, 0.0 is
	   * returned.
	   */
	  public static double safeMax(double[] v) {
		  double[] u = filterNaNAndInfinite(v);
		  if (numRows(u) == 0) return 0.0;
		  return max(u);
	  }

	  /**
	   * Returns the log of the sum of an array of numbers, which are
	   * themselves input in log form.  This is all natural logarithms.
	   * Reasonable care is taken to do this as efficiently as possible
	   * (under the assumption that the numbers might differ greatly in
	   * magnitude), with high accuracy, and without numerical overflow.
	   *
	   * @param logInputs An array of numbers [log(x1), ..., log(xn)]
	   * @return log(x1 + ... + xn)
	   */
	  public static double logSum(double[] logInputs) {
		  return logSum(logInputs,0,logInputs.length);
	  }

	  /**
	   * Returns the log of the portion between <code>fromIndex</code>, inclusive, and
	   * <code>toIndex</code>, exclusive, of an array of numbers, which are
	   * themselves input in log form.  This is all natural logarithms.
	   * Reasonable care is taken to do this as efficiently as possible
	   * (under the assumption that the numbers might differ greatly in
	   * magnitude), with high accuracy, and without numerical overflow.  Throws an
	   * {@link IllegalArgumentException} if <code>logInputs</code> is of length zero.
	   * Otherwise, returns Double.NEGATIVE_INFINITY if <code>fromIndex</code> &gt;=
	   * <code>toIndex</code>.
	   *
	   * @param logInputs An array of numbers [log(x1), ..., log(xn)]
	   * @param fromIndex The array index to start the sum from
	   * @param toIndex The array index after the last element to be summed
	   * @return log(x1 + ... + xn)
	   */
	  public static double logSum(double[] logInputs, int fromIndex, int toIndex) {
		  if (logInputs.length == 0)
			  throw new IllegalArgumentException();
		  if(fromIndex >= 0 && toIndex < logInputs.length && fromIndex >= toIndex)
			  return Double.NEGATIVE_INFINITY;
		  int maxIdx = fromIndex;
		  double max = logInputs[fromIndex];
		  for (int i = fromIndex+1; i < toIndex; i++) {
			  if (logInputs[i] > max) {
				  maxIdx = i;
				  max = logInputs[i];
			  }
		  }
		  boolean haveTerms = false;
		  double intermediate = 0.0;
		  double cutoff = max - LOGTOLERANCE;
		  // we avoid rearranging the array and so test indices each time!
		  for (int i = fromIndex; i < toIndex; i++) {
			  if (i != maxIdx && logInputs[i] > cutoff) {
				  haveTerms = true;
				  intermediate += Math.exp(logInputs[i] - max);
			  }
		  }
		  if (haveTerms) {
			  return max + Math.log(1.0 + intermediate);
		  } else {
			  return max;
		  }
	  }

	  /**
	   * Returns the log of the portion between <code>fromIndex</code>, inclusive, and
	   * <code>toIndex</code>, exclusive, of an array of numbers, which are
	   * themselves input in log form.  This is all natural logarithms.
	   * This version incorporates a stride, so you can sum only select numbers.
	   * Reasonable care is taken to do this as efficiently as possible
	   * (under the assumption that the numbers might differ greatly in
	   * magnitude), with high accuracy, and without numerical overflow.  Throws an
	   * {@link IllegalArgumentException} if <code>logInputs</code> is of length zero.
	   * Otherwise, returns Double.NEGATIVE_INFINITY if <code>fromIndex</code> &gt;=
	   * <code>toIndex</code>.
	   *
	   * @param logInputs An array of numbers [log(x1), ..., log(xn)]
	   * @param fromIndex The array index to start the sum from
	   * @param afterIndex The array index after the last element to be summed
	   * @return log(x1 + ... + xn)
	   */
	  public static double logSum(double[] logInputs, int fromIndex, int afterIndex, int stride) {
		  if (logInputs.length == 0)
			  throw new IllegalArgumentException();
		  if (fromIndex >= 0 && afterIndex < logInputs.length && fromIndex >= afterIndex)
			  return Double.NEGATIVE_INFINITY;
		  int maxIdx = fromIndex;
		  double max = logInputs[fromIndex];
		  for (int i = fromIndex + stride; i < afterIndex; i += stride) {
			  if (logInputs[i] > max) {
				  maxIdx = i;
				  max = logInputs[i];
			  }
		  }
		  boolean haveTerms = false;
		  double intermediate = 0.0;
		  double cutoff = max - LOGTOLERANCE;
		  // we avoid rearranging the array and so test indices each time!
		  for (int i = fromIndex; i < afterIndex; i += stride) {
			  if (i != maxIdx && logInputs[i] > cutoff) {
				  haveTerms = true;
				  intermediate += Math.exp(logInputs[i] - max);
			  }
		  }
		  if (haveTerms) {
			  return max + Math.log(1.0 + intermediate);  // using Math.log1p(intermediate) may be more accurate, but is slower
		  } else {
			  return max;
		  }
	  }

	  public static double logSum(List<Double> logInputs) {
		  return logSum(logInputs, 0, logInputs.size());
	  }

	  public static double logSum(List<Double> logInputs, int fromIndex, int toIndex) {
		  int length = logInputs.size();
		  if (length == 0)
			  throw new IllegalArgumentException();
		  if(fromIndex >= 0 && toIndex < length && fromIndex >= toIndex)
			  return Double.NEGATIVE_INFINITY;
		  int maxIdx = fromIndex;
		  double max = logInputs.get(fromIndex);
		  for (int i = fromIndex+1; i < toIndex; i++) {
			  double d = logInputs.get(i);
			  if (d > max) {
				  maxIdx = i;
				  max = d;
			  }
		  }
		  boolean haveTerms = false;
		  double intermediate = 0.0;
		  double cutoff = max - LOGTOLERANCE;
		  // we avoid rearranging the array and so test indices each time!
		  for (int i = fromIndex; i < toIndex; i++) {
			  double d = logInputs.get(i);
			  if (i != maxIdx && d > cutoff) {
				  haveTerms = true;
				  intermediate += Math.exp(d - max);
			  }
		  }
		  if (haveTerms) {
			  return max + Math.log(1.0 + intermediate);
		  } else {
			  return max;
		  }
	  }


	  /**
	   * Returns the log of the sum of an array of numbers, which are
	   * themselves input in log form.  This is all natural logarithms.
	   * Reasonable care is taken to do this as efficiently as possible
	   * (under the assumption that the numbers might differ greatly in
	   * magnitude), with high accuracy, and without numerical overflow.
	   *
	   * @param logInputs An array of numbers [log(x1), ..., log(xn)]
	   * @return log(x1 + ... + xn)
	   */
	  public static float logSum(float[] logInputs) {
		  int leng = logInputs.length;
		  if (leng == 0) {
			  throw new IllegalArgumentException();
		  }
		  int maxIdx = 0;
		  float max = logInputs[0];
		  for (int i = 1; i < leng; i++) {
			  if (logInputs[i] > max) {
				  maxIdx = i;
				  max = logInputs[i];
			  }
		  }
		  boolean haveTerms = false;
		  double intermediate = 0.0f;
		  float cutoff = max - LOGTOLERANCE_F;
		  // we avoid rearranging the array and so test indices each time!
		  for (int i = 0; i < leng; i++) {
			  if (i != maxIdx && logInputs[i] > cutoff) {
				  haveTerms = true;
				  intermediate += Math.exp(logInputs[i] - max);
			  }
		  }
		  if (haveTerms) {
			  return max + (float) Math.log(1.0 + intermediate);
		  } else {
			  return max;
		  }
	  }

	  // LINEAR ALGEBRAIC FUNCTIONS

	  public static double innerProduct(double[] a, double[] b) {
		  double result = 0.0;
		  int len = Math.min(a.length, b.length);
		  for (int i = 0; i < len; i++) {
			  result += a[i] * b[i];
		  }
		  return result;
	  }

	  public static double innerProduct(float[] a, float[] b) {
		  double result = 0.0;
		  int len = Math.min(a.length, b.length);
		  for (int i = 0; i < len; i++) {
			  result += a[i] * b[i];
		  }
		  return result;
	  }

	  // UTILITIES

	  public static int[] subArray(int[] a, int from, int to) {
		  int[] result = new int[to-from];
		  System.arraycopy(a, from, result, 0, to-from);
		  return result;
	  }

	  public static double[][] load2DMatrixFromFile(String filename) throws IOException {
		  String s = IOUtils.slurpFile(filename);
		  String[] rows = s.split("[\r\n]+");
		  double[][] result = new double[rows.length][];
		  for (int i=0; i<result.length; i++) {
			  String[] columns = rows[i].split("\\s+");
			  result[i] = new double[columns.length];
			  for (int j=0; j<result[i].length; j++) {
				  result[i][j] = Double.parseDouble(columns[j]);
			  }
		  }
		  return result;
	  }

	  public static Integer[] box(int[] assignment) {
		  Integer[] result = new Integer[assignment.length];
		  for (int i=0; i<assignment.length; i++) {
			  result[i] = Integer.valueOf(assignment[i]);
		  }
		  return result;
	  }

	  public static int[] unboxToInt(Collection<Integer> list) {
		  int[] result = new int[list.size()];
		  int i = 0;
		  for (int v : list) {
			  result[i++] = v;
		  }
		  return result;
	  }

	  public static Double[] box(double[] assignment) {
		  Double[] result = new Double[assignment.length];
		  for (int i=0; i<assignment.length; i++) {
			  result[i] = Double.valueOf(assignment[i]);
		  }
		  return result;
	  }

	  public static double[] unbox(Collection<Double> list) {
		  double[] result = new double[list.size()];
		  int i = 0;
		  for (double v : list) {
			  result[i++] = v;
		  }
		  return result;
	  }

	  public static int indexOf(int n, int[] a) {
		  for (int i=0; i<a.length; i++) {
			  if (a[i]==n) return i;
		  }
		  return -1;
	  }

	  public static int[][] castToInt(double[][] doubleCounts) {
		  int[][] result = new int[doubleCounts.length][];
		  for (int i=0; i<doubleCounts.length; i++) {
			  result[i] = new int[doubleCounts[i].length];
			  for (int j=0; j<doubleCounts[i].length; j++) {
				  result[i][j] = (int) doubleCounts[i][j];
			  }
		  }
		  return result;
	  }

	  // PROBABILITY FUNCTIONS

	  /**
	   * Makes the values in this array sum to 1.0. Does it in place.
	   * If the total is 0.0 or NaN, throws an RuntimeException.
	   */
	  public static void normalize(double[] a) {
		  double total = sum(a);
		  if (total == 0.0 || Double.isNaN(total)) {
			  throw new RuntimeException("Can't normalize an array with sum 0.0 or NaN: " + Arrays.toString(a));
		  }
		  multiplyInPlace(a, 1.0/total); // divide each value by total
	  }
	  
	  /** added by BTO */
	  public static void normalize(double[][] a) {
		  double total = sum(a);
		  if (total == 0.0 || Double.isNaN(total)) {
			  throw new RuntimeException("Can't normalize an array with sum 0.0 or NaN: " + Arrays.toString(a));
		  }
		  multiplyInPlace(a, 1.0/total); // divide each value by total		  
	  }

	  public static void L1normalize(double[] a) {
		  double total = L1Norm(a);
		  if (total == 0.0 || Double.isNaN(total)) {
			  if (a.length < 100) {
				  throw new RuntimeException("Can't normalize an array with sum 0.0 or NaN: " + Arrays.toString(a));
			  } else {
				  double[] aTrunc = new double[100];
				  System.arraycopy(a, 0, aTrunc, 0, 100);
				  throw new RuntimeException("Can't normalize an array with sum 0.0 or NaN: " + Arrays.toString(aTrunc) + " ... ");
			  }

		  }
		  multiplyInPlace(a, 1.0/total); // divide each value by total
	  }

	  /**
	   * Makes the values in this array sum to 1.0. Does it in place.
	   * If the total is 0.0 or NaN, throws an RuntimeException.
	   */
	  public static void normalize(float[] a) {
		  float total = sum(a);
		  if (total == 0.0 || Double.isNaN(total)) {
			  throw new RuntimeException("Can't normalize an array with sum 0.0 or NaN");
		  }
		  multiplyInPlace(a, 1.0/total); // divide each value by total
	  }

	  /**
	   * Standardize values in this array, i.e., subtract the mean and divide by the standard deviation.
	   * If standard deviation is 0.0, throws an RuntimeException.
	   */
	  public static void standardize(double[] a) {
		  double m = mean(a);
		  if (Double.isNaN(m))
			  throw new RuntimeException("Can't standardize array whose mean is NaN");
		  double s = stdev(a);
		  if(s == 0.0 || Double.isNaN(s))
			  throw new RuntimeException("Can't standardize array whose standard deviation is 0.0 or NaN");
		  addInPlace(a, -m); // subtract mean
		  multiplyInPlace(a, 1.0/s); // divide by standard deviation
	  }

	  /** BTO: this is in stanford but repetitve with norm() */
	  public static double L2Norm(double[] a) {
		  double result = 0.0;
		  for(double d: a) {
			  result += Math.pow(d,2);
		  }
		  return Math.sqrt(result);
	  }

	  public static double L1Norm(double[] a) {
		  double result = 0.0;
		  for (double d: a) {
			  result += Math.abs(d);
		  }
		  return result;
	  }
	  /** added by BTO */
	  public static double L2Dist(double[] a, double[] b) {
		  return L2Norm(pairwiseSubtract(a,b));
	  }
	  /** added by BTO */
	  public static double L1Dist(double[] a, double[] b) {
		  return L1Norm(pairwiseSubtract(a,b));
	  }

	  /**
	   * Makes the values in this array sum to 1.0. Does it in place.
	   * If the total is 0.0, throws a RuntimeException.
	   * If the total is Double.NEGATIVE_INFINITY, then it replaces the
	   * array with a normalized uniform distribution. CDM: This last bit is
	   * weird!  Do we really want that?
	   */
	  public static void logNormalize(double[] a) {
		  double logTotal = logSum(a);
		  if (logTotal == Double.NEGATIVE_INFINITY) {
			  // to avoid NaN values
			  double v = -Math.log(a.length);
			  for (int i = 0; i < a.length; i++) {
				  a[i] = v;
			  }
			  return;
		  }
		  addInPlace(a, -logTotal); // subtract log total from each value
	  }

	  // BTO: deleting the sampleFromDistribution() methods in lieu of FastRandom and Mallet Randoms

	  //	  /**
	  //	   * Samples from the distribution over values 0 through d.length given by d.
	  //	   * Assumes that the distribution sums to 1.0.
	  //	   *
	  //	   * @param d the distribution to sample from
	  //	   * @return a value from 0 to d.length
	  //	   */
	  //	  public static int sampleFromDistribution(double[] d) {
	  //	    return sampleFromDistribution(d, rand);
	  //	  }
	  //
	  //	  /**
	  //	   * Samples from the distribution over values 0 through d.length given by d.
	  //	   * Assumes that the distribution sums to 1.0.
	  //	   *
	  //	   * @param d the distribution to sample from
	  //	   * @return a value from 0 to d.length
	  //	   */
	  //	  public static int sampleFromDistribution(double[] d, Random random) {
	  //	    // sample from the uniform [0,1]
	  //	    double r = random.nextDouble();
	  //	    // now compare its value to cumulative values to find what interval it falls in
	  //	    double total = 0;
	  //	    for (int i = 0; i < d.length - 1; i++) {
	  //	      if (Double.isNaN(d[i])) {
	  //	        throw new RuntimeException("Can't sample from NaN");
	  //	      }
	  //	      total += d[i];
	  //	      if (r < total) {
	  //	        return i;
	  //	      }
	  //	    }
	  //	    return d.length - 1; // in case the "double-math" didn't total to exactly 1.0
	  //	  }
	  //
	  //	  /**
	  //	   * Samples from the distribution over values 0 through d.length given by d.
	  //	   * Assumes that the distribution sums to 1.0.
	  //	   *
	  //	   * @param d the distribution to sample from
	  //	   * @return a value from 0 to d.length
	  //	   */
	  //	  public static int sampleFromDistribution(float[] d, Random random) {
	  //	    // sample from the uniform [0,1]
	  //	    double r = random.nextDouble();
	  //	    // now compare its value to cumulative values to find what interval it falls in
	  //	    double total = 0;
	  //	    for (int i = 0; i < d.length - 1; i++) {
	  //	      if (Float.isNaN(d[i])) {
	  //	        throw new RuntimeException("Can't sample from NaN");
	  //	      }
	  //	      total += d[i];
	  //	      if (r < total) {
	  //	        return i;
	  //	      }
	  //	    }
	  //	    return d.length - 1; // in case the "double-math" didn't total to exactly 1.0
	  //	  }

	  public static double klDivergence(double[] from, double[] to) {
		  double kl = 0.0;
		  double tot = sum(from);
		  double tot2 = sum(to);
		  // System.out.println("tot is " + tot + " tot2 is " + tot2);
		  for (int i = 0; i < from.length; i++) {
			  if (from[i] == 0.0) {
				  continue;
			  }
			  double num = from[i] / tot;
			  double num2 = to[i] / tot2;
			  // System.out.println("num is " + num + " num2 is " + num2);
			  kl += num * (Math.log(num / num2) / Math.log(2.0));
		  }
		  return kl;
	  }

	  /**
	   * Returns the Jensen Shannon divergence (information radius) between
	   * a and b, defined as the average of the kl divergences from a to b
	   * and from b to a.
	   */
	  public static double jensenShannonDivergence(double[] a, double[] b) {
		  double[] average = pairwiseAdd(a, b);
		  multiplyInPlace(average, .5);
		  return .5 * klDivergence(a, average) + .5 * klDivergence(b, average);
	  }

	  public static void setToLogDeterministic(float[] a, int i) {
		  for (int j = 0; j < a.length; j++) {
			  if (j == i) {
				  a[j] = 0.0F;
			  } else {
				  a[j] = Float.NEGATIVE_INFINITY;
			  }
		  }
	  }

	  public static void setToLogDeterministic(double[] a, int i) {
		  for (int j = 0; j < a.length; j++) {
			  if (j == i) {
				  a[j] = 0.0;
			  } else {
				  a[j] = Double.NEGATIVE_INFINITY;
			  }
		  }
	  }

	  // SAMPLE ANALYSIS

	  public static double mean(double[] a) {
		  return sum(a) / a.length;
	  }

	  public static double median(double[] a) {
		  double[] b = new double[a.length];
		  System.arraycopy(a, 0, b, 0, b.length);
		  Arrays.sort(b);
		  int mid = b.length / 2;
		  if (b.length % 2 == 0) {
			  return (b[mid - 1] + b[mid]) / 2.0;
		  } else {
			  return b[mid];
		  }
	  }

	  /**
	   * Returns the mean of a vector of doubles.  Any values which are NaN or
	   * infinite are ignored.  If the vector is empty, 0.0 is returned.
	   */
	  public static double safeMean(double[] v) {
		  double[] u = filterNaNAndInfinite(v);
		  if (numRows(u) == 0) return 0.0;
		  return mean(u);
	  }

	  public static double sumSquaredError(double[] a) {
		  double mean = mean(a);
		  double result = 0.0;
		  for (double anA : a) {
			  double diff = anA - mean;
			  result += (diff * diff);
		  }
		  return result;
	  }

	  public static double sumSquared(double[] a) {
		  double result = 0.0;
		  for (double anA : a) {
			  result += (anA * anA);
		  }
		  return result;
	  }

	  public static double varianceMLE(double[] a) {
		  return sumSquaredError(a) / a.length;
	  }

	  public static double variance(double[] a) {
		  return sumSquaredError(a) / (a.length - 1);
	  }

	  public static double stdev(double[] a) {
		  return Math.sqrt(variance(a));
	  }

	  /**
	   * Returns the standard deviation of a vector of doubles.  Any values which
	   * are NaN or infinite are ignored.  If the vector contains fewer than two
	   * values, 1.0 is returned.
	   */
	  public static double safeStdev(double[] v) {
		  double[] u = filterNaNAndInfinite(v);
		  if (numRows(u) < 2) return 1.0;
		  return stdev(u);
	  }

	  public static double standardErrorOfMean(double[] a) {
		  return stdev(a) / Math.sqrt(a.length);
	  }


	  /**
	   * Fills the array with sample from 0 to numArgClasses-1 without replacement.
	   */
	  public static void sampleWithoutReplacement(int[] array, int numArgClasses) {
		  sampleWithoutReplacement(array, numArgClasses, rand);
	  }
	  /**
	   * Fills the array with sample from 0 to numArgClasses-1 without replacement.
	   * BTO: bugfixed the case when desired samplesize > population size, but solution is undesirable (only fill the prefix).
	   * @see sampleWithoutReplacement(int,int) for a safer alternative.
	   */
	  public static void sampleWithoutReplacement(int[] array, int numArgClasses, Random rand) {
		  int[] temp = new int[numArgClasses];
		  for (int i = 0; i < temp.length; i++) {
			  temp[i] = i;
		  }
		  shuffle(temp, rand);
		  // BTO: bugfix adding Math.min().  So if user passes in array that's longer than the population size,
		  // just only fill up the prefix.  not much else you can do at this point, but at least don't crash.
		  // See my {@link sampleWithoutReplacement(int,int)} for a safer alternative.
		  System.arraycopy(temp, 0, array, 0, Math.min(array.length, temp.length));
	  }

	  //	  public static void main(String args[]) {
	  //		int[] out = new int[Integer.valueOf(args[0])];
	  //		Arr.sampleWithoutReplacement(out, Integer.valueOf(args[1]));
	  //		U.p(out);
	  //	  }


	  public static void shuffle(int[] a) {
		  shuffle(a, rand);
	  }

	  public static void shuffle(int[] a, Random rand) {
		  for (int i=a.length-1; i>=1; i--) {
			  int j = rand.nextInt(i+1); // a random index from 0 to i inclusive, may shuffle with itself
			  int tmp = a[i];
			  a[i] = a[j];
			  a[j] = tmp;
		  }
	  }

	  public static void reverse(int[] a) {
		  for (int i=0; i<a.length/2; i++) {
			  int j = a.length - i - 1;
			  int tmp = a[i];
			  a[i] = a[j];
			  a[j] = tmp;
		  }
	  }

	  public static boolean contains(int[] a, int i) {
		  for (int k : a) {
			  if (k == i) return true;
		  }
		  return false;
	  }

	  public static boolean containsInSubarray(int[] a, int begin, int end, int i) {
		  for (int j = begin; j < end; j++) {
			  if (a[j]==i) return true;
		  }
		  return false;
	  }

	  /**
	   * Direct computation of Pearson product-moment correlation coefficient.
	   * Note that if x and y are involved in several computations of
	   * pearsonCorrelation, it is perhaps more advisable to first standardize
	   * x and y, then compute innerProduct(x,y)/(x.length-1).
	   */
	  public static double pearsonCorrelation(double[] x, double[] y) {
		  double result;
		  double sum_sq_x = 0, sum_sq_y = 0;
		  double mean_x = x[0], mean_y = y[0];
		  double sum_coproduct = 0;
		  for(int i=2; i<x.length+1;++i) {
			  double w = (i - 1)*1.0/i;
			  double delta_x = x[i-1] - mean_x;
			  double delta_y = y[i-1] - mean_y;
			  sum_sq_x += delta_x * delta_x*w;
			  sum_sq_y += delta_y * delta_y*w;
			  sum_coproduct += delta_x * delta_y*w;
			  mean_x += delta_x / i;
			  mean_y += delta_y / i;
		  }
		  double pop_sd_x = Math.sqrt(sum_sq_x/x.length);
		  double pop_sd_y = Math.sqrt(sum_sq_y/y.length);
		  double cov_x_y = sum_coproduct / x.length;
		  double denom = pop_sd_x*pop_sd_y;
		  if(denom == 0.0)
			  return 0.0;
		  result = cov_x_y/denom;
		  return result;
	  }

	  /**
	   * Computes the significance level by approximate randomization, using a
	   * default value of 1000 iterations.  See documentation for other version
	   * of method.
	   */
	  public static double sigLevelByApproxRand(double[] A, double[] B) {
		  return sigLevelByApproxRand(A, B, 1000);
	  }

	  /**
	   * Takes a pair of arrays, A and B, which represent corresponding
	   * outcomes of a pair of random variables: say, results for two different
	   * classifiers on a sequence of inputs.  Returns the estimated
	   * probability that the difference between the means of A and B is not
	   * significant, that is, the significance level.  This is computed by
	   * "approximate randomization".  The test statistic is the absolute
	   * difference between the means of the two arrays.  A randomized test
	   * statistic is computed the same way after initially randomizing the
	   * arrays by swapping each pair of elements with 50% probability.  For
	   * the given number of iterations, we generate a randomized test
	   * statistic and compare it to the actual test statistic.  The return
	   * value is the proportion of iterations in which a randomized test
	   * statistic was found to exceed the actual test statistic.
	   *
	   * @param A Outcome of one r.v.
	   * @param B Outcome of another r.v.
	   * @return Significance level by randomization
	   */
	  public static double sigLevelByApproxRand(double[] A, double[] B, int iterations) {
		  if (A.length == 0)
			  throw new IllegalArgumentException("Input arrays must not be empty!");
		  if (A.length != B.length)
			  throw new IllegalArgumentException("Input arrays must have equal length!");
		  if (iterations <= 0)
			  throw new IllegalArgumentException("Number of iterations must be positive!");
		  double testStatistic = absDiffOfMeans(A, B, false); // not randomized
		  int successes = 0;
		  for (int i = 0; i < iterations; i++) {
			  double t =  absDiffOfMeans(A, B, true); // randomized
			  if (t >= testStatistic) successes++;
		  }
		  return (double) (successes + 1) / (double) (iterations + 1);
	  }

	  public static double sigLevelByApproxRand(int[] A, int[] B) {
		  return sigLevelByApproxRand(A, B, 1000);
	  }

	  public static double sigLevelByApproxRand(int[] A, int[] B, int iterations) {
		  if (A.length == 0)
			  throw new IllegalArgumentException("Input arrays must not be empty!");
		  if (A.length != B.length)
			  throw new IllegalArgumentException("Input arrays must have equal length!");
		  if (iterations <= 0)
			  throw new IllegalArgumentException("Number of iterations must be positive!");
		  double[] X = new double[A.length];
		  double[] Y = new double[B.length];
		  for (int i = 0; i < A.length; i++) {
			  X[i] = A[i];
			  Y[i] = B[i];
		  }
		  return sigLevelByApproxRand(X, Y, iterations);
	  }

	  public static double sigLevelByApproxRand(boolean[] A, boolean[] B) {
		  return sigLevelByApproxRand(A, B, 1000);
	  }

	  public static double sigLevelByApproxRand(boolean[] A, boolean[] B, int iterations) {
		  if (A.length == 0)
			  throw new IllegalArgumentException("Input arrays must not be empty!");
		  if (A.length != B.length)
			  throw new IllegalArgumentException("Input arrays must have equal length!");
		  if (iterations <= 0)
			  throw new IllegalArgumentException("Number of iterations must be positive!");
		  double[] X = new double[A.length];
		  double[] Y = new double[B.length];
		  for (int i = 0; i < A.length; i++) {
			  X[i] = (A[i] ? 1.0 : 0.0);
			  Y[i] = (B[i] ? 1.0 : 0.0);
		  }
		  return sigLevelByApproxRand(X, Y, iterations);
	  }


	  // Returns the absolute difference between the means of arrays A and B.
	  // If 'randomize' is true, swaps matched A & B entries with 50% probability
	  // Assumes input arrays have equal, non-zero length.
	  private static double absDiffOfMeans(double[] A, double[] B, boolean randomize) {
		  Random random = new Random();
		  double aTotal = 0.0;
		  double bTotal = 0.0;
		  for (int i = 0; i < A.length; i++) {
			  if (randomize && random.nextBoolean()) {
				  aTotal += B[i];
				  bTotal += A[i];
			  } else {
				  aTotal += A[i];
				  bTotal += B[i];
			  }
		  }
		  double aMean = aTotal / A.length;
		  double bMean = bTotal / B.length;
		  return Math.abs(aMean - bMean);
	  }

	  // PRINTING FUNCTIONS

	  public static String toBinaryString(byte[] b) {
		  StringBuilder s = new StringBuilder();
		  for (byte by : b) {
			  for (int j = 7; j >= 0; j--) {
				  if ((by & (1 << j)) > 0) {
					  s.append('1');
				  } else {
					  s.append('0');
				  }
			  }
			  s.append(' ');
		  }
		  return s.toString();
	  }

	  //	  public static String toString(double[] a) {
	  //	    return toString(a, null);
	  //	  }

	  public static String toString(double[] a, NumberFormat nf) {
		  if (a == null) return null;
		  if (a.length == 0) return "[]";
		  StringBuilder b = new StringBuilder();
		  b.append('[');
		  for (int i = 0; i < a.length - 1; i++) {
			  String s;
			  if (nf == null) {
				  s = String.valueOf(a[i]);
			  } else {
				  s = nf.format(a[i]);
			  }
			  b.append(s);
			  b.append(", ");
		  }
		  String s;
		  if (nf == null) {
			  s = String.valueOf(a[a.length - 1]);
		  } else {
			  s = nf.format(a[a.length - 1]);
		  }
		  b.append(s);
		  b.append(']');
		  return b.toString();
	  }

	  //	  public static String toString(float[] a) {
	  //	    return toString(a, null);
	  //	  }

	  public static String toString(float[] a, NumberFormat nf) {
		  if (a == null) return null;
		  if (a.length == 0) return "[]";
		  StringBuilder b = new StringBuilder();
		  b.append('[');
		  for (int i = 0; i < a.length - 1; i++) {
			  String s;
			  if (nf == null) {
				  s = String.valueOf(a[i]);
			  } else {
				  s = nf.format(a[i]);
			  }
			  b.append(s);
			  b.append(", ");
		  }
		  String s;
		  if (nf == null) {
			  s = String.valueOf(a[a.length - 1]);
		  } else {
			  s = nf.format(a[a.length - 1]);
		  }
		  b.append(s);
		  b.append(']');
		  return b.toString();
	  }

	  //	  public static String toString(int[] a) {
	  //	    return toString(a, null);
	  //	  }

	  public static String toString(int[] a, NumberFormat nf) {
		  if (a == null) return null;
		  if (a.length == 0) return "[]";
		  StringBuilder b = new StringBuilder();
		  b.append('[');
		  for (int i = 0; i < a.length - 1; i++) {
			  String s;
			  if (nf == null) {
				  s = String.valueOf(a[i]);
			  } else {
				  s = nf.format(a[i]);
			  }
			  b.append(s);
			  b.append(", ");
		  }
		  String s;
		  if (nf == null) {
			  s = String.valueOf(a[a.length - 1]);
		  } else {
			  s = nf.format(a[a.length - 1]);
		  }
		  b.append(s);
		  b.append(']');
		  return b.toString();
	  }

	  //	  public static String toString(byte[] a) {
	  //	    return toString(a, null);
	  //	  }

	  public static String toString(byte[] a, NumberFormat nf) {
		  if (a == null) return null;
		  if (a.length == 0) return "[]";
		  StringBuilder b = new StringBuilder();
		  b.append('[');
		  for (int i = 0; i < a.length - 1; i++) {
			  String s;
			  if (nf == null) {
				  s = String.valueOf(a[i]);
			  } else {
				  s = nf.format(a[i]);
			  }
			  b.append(s);
			  b.append(", ");
		  }
		  String s;
		  if (nf == null) {
			  s = String.valueOf(a[a.length - 1]);
		  } else {
			  s = nf.format(a[a.length - 1]);
		  }
		  b.append(s);
		  b.append(']');
		  return b.toString();
	  }

	  public static String toString(int[][] counts, Object[] rowLabels, Object[] colLabels, int labelSize, int cellSize, NumberFormat nf, boolean printTotals) {
		  // first compute row totals and column totals
		  if (counts.length==0 || counts[0].length==0) return "";
		  int[] rowTotals = new int[counts.length];
		  int[] colTotals = new int[counts[0].length]; // assume it's square
		  int total = 0;
		  for (int i = 0; i < counts.length; i++) {
			  for (int j = 0; j < counts[i].length; j++) {
				  rowTotals[i] += counts[i][j];
				  colTotals[j] += counts[i][j];
				  total += counts[i][j];
			  }
		  }
		  StringBuilder result = new StringBuilder();
		  // column labels
		  if (colLabels != null) {
			  result.append(StringUtils.padLeft("", labelSize)); // spacing for the row labels!
			  for (int j = 0; j < counts[0].length; j++) {
				  String s = (colLabels[j]==null ? "null" : colLabels[j].toString());
				  if (s.length() > cellSize - 1) {
					  s = s.substring(0, cellSize - 1);
				  }
				  s = StringUtils.padLeft(s, cellSize);
				  result.append(s);
			  }
			  if (printTotals) {
				  result.append(StringUtils.padLeftOrTrim("Total", cellSize));
			  }
			  result.append('\n');
		  }
		  for (int i = 0; i < counts.length; i++) {
			  // row label
			  if (rowLabels != null) {
				  String s = (rowLabels[i]==null ? "null" : rowLabels[i].toString());
				  s = StringUtils.padOrTrim(s, labelSize); // left align this guy only
				  result.append(s);
			  }
			  // value
			  for (int j = 0; j < counts[i].length; j++) {
				  result.append(StringUtils.padLeft(nf.format(counts[i][j]), cellSize));
			  }
			  // the row total
			  if (printTotals) {
				  result.append(StringUtils.padLeft(nf.format(rowTotals[i]), cellSize));
			  }
			  result.append('\n');
		  }
		  // the col totals
		  if (printTotals) {
			  result.append(StringUtils.pad("Total", labelSize));
			  for (int colTotal : colTotals) {
				  result.append(StringUtils.padLeft(nf.format(colTotal), cellSize));
			  }
			  result.append(StringUtils.padLeft(nf.format(total), cellSize));
		  }
		  return result.toString();
	  }


	  public static String toString(double[][] counts) {
		  return toString(counts, 10, null, null, NumberFormat.getInstance(), false);
	  }

	  public static String toString(double[][] counts, int cellSize, Object[] rowLabels, Object[] colLabels, NumberFormat nf, boolean printTotals) {
		  if (counts==null) return null;
		  // first compute row totals and column totals
		  double[] rowTotals = new double[counts.length];
		  double[] colTotals = new double[counts[0].length]; // assume it's square
		  double total = 0.0;
		  for (int i = 0; i < counts.length; i++) {
			  for (int j = 0; j < counts[i].length; j++) {
				  rowTotals[i] += counts[i][j];
				  colTotals[j] += counts[i][j];
				  total += counts[i][j];
			  }
		  }
		  StringBuilder result = new StringBuilder();
		  // column labels
		  if (colLabels != null) {
			  result.append(StringUtils.padLeft("", cellSize));
			  for (int j = 0; j < counts[0].length; j++) {
				  String s = colLabels[j].toString();
				  if (s.length() > cellSize - 1) {
					  s = s.substring(0, cellSize - 1);
				  }
				  s = StringUtils.padLeft(s, cellSize);
				  result.append(s);
			  }
			  if (printTotals) {
				  result.append(StringUtils.padLeftOrTrim("Total", cellSize));
			  }
			  result.append('\n');
		  }
		  for (int i = 0; i < counts.length; i++) {
			  // row label
			  if (rowLabels != null) {
				  String s = rowLabels[i].toString();
				  s = StringUtils.padOrTrim(s, cellSize); // left align this guy only
				  result.append(s);
			  }
			  // value
			  for (int j = 0; j < counts[i].length; j++) {
				  result.append(StringUtils.padLeft(nf.format(counts[i][j]), cellSize));
			  }
			  // the row total
			  if (printTotals) {
				  result.append(StringUtils.padLeft(nf.format(rowTotals[i]), cellSize));
			  }
			  result.append('\n');
		  }
		  // the col totals
		  if (printTotals) {
			  result.append(StringUtils.pad("Total", cellSize));
			  for (double colTotal : colTotals) {
				  result.append(StringUtils.padLeft(nf.format(colTotal), cellSize));
			  }
			  result.append(StringUtils.padLeft(nf.format(total), cellSize));
		  }
		  return result.toString();
	  }

	  public static String toString(float[][] counts) {
		  return toString(counts, 10, null, null, NumberFormat.getIntegerInstance(), false);
	  }

	  public static String toString(float[][] counts, int cellSize, Object[] rowLabels, Object[] colLabels, NumberFormat nf, boolean printTotals) {
		  // first compute row totals and column totals
		  double[] rowTotals = new double[counts.length];
		  double[] colTotals = new double[counts[0].length]; // assume it's square
		  double total = 0.0;
		  for (int i = 0; i < counts.length; i++) {
			  for (int j = 0; j < counts[i].length; j++) {
				  rowTotals[i] += counts[i][j];
				  colTotals[j] += counts[i][j];
				  total += counts[i][j];
			  }
		  }
		  StringBuilder result = new StringBuilder();
		  // column labels
		  if (colLabels != null) {
			  result.append(StringUtils.padLeft("", cellSize));
			  for (int j = 0; j < counts[0].length; j++) {
				  String s = colLabels[j].toString();
				  s = StringUtils.padLeftOrTrim(s, cellSize);
				  result.append(s);
			  }
			  if (printTotals) {
				  result.append(StringUtils.padLeftOrTrim("Total", cellSize));
			  }
			  result.append('\n');
		  }
		  for (int i = 0; i < counts.length; i++) {
			  // row label
			  if (rowLabels != null) {
				  String s = rowLabels[i].toString();
				  s = StringUtils.pad(s, cellSize); // left align this guy only
				  result.append(s);
			  }
			  // value
			  for (int j = 0; j < counts[i].length; j++) {
				  result.append(StringUtils.padLeft(nf.format(counts[i][j]), cellSize));
			  }
			  // the row total
			  if (printTotals) {
				  result.append(StringUtils.padLeft(nf.format(rowTotals[i]), cellSize));
			  }
			  result.append('\n');
		  }
		  // the col totals
		  if (printTotals) {
			  result.append(StringUtils.pad("Total", cellSize));
			  for (double colTotal : colTotals) {
				  result.append(StringUtils.padLeft(nf.format(colTotal), cellSize));
			  }
			  result.append(StringUtils.padLeft(nf.format(total), cellSize));
		  }
		  return result.toString();
	  }

	  /**
	   * For testing only.
	   * @param args Ignored
	   */
	  //	  public static void main(String[] args) {
	  //	    Random random = new Random();
	  //	    int length = 100;
	  //	    double[] A = new double[length];
	  //	    double[] B = new double[length];
	  //	    double aAvg = 70.0;
	  //	    double bAvg = 70.5;
	  //	    for (int i = 0; i < length; i++) {
	  //	      A[i] = aAvg + random.nextGaussian();
	  //	      B[i] = bAvg + random.nextGaussian();
	  //	    }
	  //	    System.out.println("A has length " + A.length + " and mean " + mean(A));
	  //	    System.out.println("B has length " + B.length + " and mean " + mean(B));
	  //	    for (int t = 0; t < 10; t++) {
	  //	      System.out.println("p-value: " + sigLevelByApproxRand(A, B));
	  //	    }
	  //	  }

	  public static int[][] deepCopy(int[][] counts) {
		  int[][] result = new int[counts.length][];
		  for (int i=0; i<counts.length; i++) {
			  result[i] = new int[counts[i].length];
			  System.arraycopy(counts[i], 0, result[i], 0, counts[i].length);
		  }
		  return result;
	  }

	  public static double[][] covariance(double[][] data) {
		  double[] means = new double[data.length];
		  for (int i = 0; i < means.length; i++) {
			  means[i] = mean(data[i]);
		  }

		  double[][] covariance = new double[means.length][means.length];
		  for (int i = 0; i < data[0].length; i++) {
			  for (int j = 0; j < means.length; j++) {
				  for (int k = 0; k < means.length; k++) {
					  covariance[j][k] += (means[j]-data[j][i])*(means[k]-data[k][i]);
				  }
			  }
		  }

		  for (int i = 0; i < covariance.length; i++) {
			  for (int j = 0; j < covariance[i].length; j++) {
				  covariance[i][j] = Math.sqrt(covariance[i][j])/(data[0].length);
			  }
		  }
		  return covariance;
	  }


	  public static void addMultInto(double[] a, double[] b, double[] c, double d) {
		  for (int i=0; i<a.length; i++) {
			  a[i] = b[i] + c[i] * d;
		  }
	  }

	  public static void multiplyInto(double[] a, double[] b, double c) {
		  for (int i=0; i<a.length; i++) {
			  a[i] = b[i] * c;
		  }
	  }

	  //	  /**
	  //	   * Simulate Arrays.copyOf method provided by Java 6
	  //	   * When/if the JavaNLP-core code base moves past Java 5, this method can be removed
	  //	   *
	  //	   * @param original
	  //	   * @param newSize
	  //	   */
	  //	  public static double[] copyOf(double[] original, int newSize) {
	  //	     double[] a = new double[newSize];
	  //	     System.arraycopy(original, 0, a, 0, original.length);
	  //	     return a;
	  //	  }

	  ////////////////   EXTERNAL from Java SDK's Arrays, version 1.6

	  /*
	   * This class contains various methods for manipulating arrays (such as
	   * sorting and searching).  This class also contains a static factory
	   * that allows arrays to be viewed as lists.
	   *
	   * <p>The methods in this class all throw a <tt>NullPointerException</tt> if
	   * the specified array reference is null, except where noted.
	   *
	   * <p>The documentation for the methods contained in this class includes
	   * briefs description of the <i>implementations</i>.  Such descriptions should
	   * be regarded as <i>implementation notes</i>, rather than parts of the
	   * <i>specification</i>.  Implementors should feel free to substitute other
	   * algorithms, so long as the specification itself is adhered to.  (For
	   * example, the algorithm used by <tt>sort(Object[])</tt> does not have to be
	   * a mergesort, but it does have to be <i>stable</i>.)
	   *
	   * <p>This class is a member of the
	   * <a href="{@docRoot}/../technotes/guides/collections/index.html">
	   * Java Collections Framework</a>.
	   *
	   * @author  Josh Bloch
	   * @author  Neal Gafter
	   * @author  John Rose
	   * @version %I%, %G%
	   * @since   1.2
	   */

	  // Sorting

	  /**
	   * Sorts the specified array of longs into ascending numerical order.
	   * The sorting algorithm is a tuned quicksort, adapted from Jon
	   * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	   * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	   * 1993).  This algorithm offers n*log(n) performance on many data sets
	   * that cause other quicksorts to degrade to quadratic performance.
	   *
	   * @param a the array to be sorted
	   */
	  public static void sort(long[] a) {
		  sort1(a, 0, a.length);
	  }

	  /**
	   * Sorts the specified range of the specified array of longs into
	   * ascending numerical order.  The range to be sorted extends from index
	   * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
	   * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)
	   *
	   * <p>The sorting algorithm is a tuned quicksort, adapted from Jon
	   * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	   * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	   * 1993).  This algorithm offers n*log(n) performance on many data sets
	   * that cause other quicksorts to degrade to quadratic performance.
	   *
	   * @param a the array to be sorted
	   * @param fromIndex the index of the first element (inclusive) to be
	   *        sorted
	   * @param toIndex the index of the last element (exclusive) to be sorted
	   * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	   * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	   * <tt>toIndex &gt; a.length</tt>
	   */
	  public static void sort(long[] a, int fromIndex, int toIndex) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  sort1(a, fromIndex, toIndex-fromIndex);
	  }

	  /**
	   * Sorts the specified array of ints into ascending numerical order.
	   * The sorting algorithm is a tuned quicksort, adapted from Jon
	   * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	   * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	   * 1993).  This algorithm offers n*log(n) performance on many data sets
	   * that cause other quicksorts to degrade to quadratic performance.
	   *
	   * @param a the array to be sorted
	   */
	  public static void sort(int[] a) {
		  sort1(a, 0, a.length);
	  }

	  /**
	   * Sorts the specified range of the specified array of ints into
	   * ascending numerical order.  The range to be sorted extends from index
	   * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
	   * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)<p>
	   *
	   * The sorting algorithm is a tuned quicksort, adapted from Jon
	   * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	   * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	   * 1993).  This algorithm offers n*log(n) performance on many data sets
	   * that cause other quicksorts to degrade to quadratic performance.
	   *
	   * @param a the array to be sorted
	   * @param fromIndex the index of the first element (inclusive) to be
	   *        sorted
	   * @param toIndex the index of the last element (exclusive) to be sorted
	   * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	   * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	   *	       <tt>toIndex &gt; a.length</tt>
	   */
	  public static void sort(int[] a, int fromIndex, int toIndex) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  sort1(a, fromIndex, toIndex-fromIndex);
	  }

	  /**
	   * Sorts the specified array of shorts into ascending numerical order.
	   * The sorting algorithm is a tuned quicksort, adapted from Jon
	   * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	   * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	   * 1993).  This algorithm offers n*log(n) performance on many data sets
	   * that cause other quicksorts to degrade to quadratic performance.
	   *
	   * @param a the array to be sorted
	   */
	  public static void sort(short[] a) {
		  sort1(a, 0, a.length);
	  }

	  /**
	   * Sorts the specified range of the specified array of shorts into
	   * ascending numerical order.  The range to be sorted extends from index
	   * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
	   * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)<p>
	   *
	   * The sorting algorithm is a tuned quicksort, adapted from Jon
	   * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	   * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	   * 1993).  This algorithm offers n*log(n) performance on many data sets
	   * that cause other quicksorts to degrade to quadratic performance.
	   *
	   * @param a the array to be sorted
	   * @param fromIndex the index of the first element (inclusive) to be
	   *        sorted
	   * @param toIndex the index of the last element (exclusive) to be sorted
	   * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	   * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	   *	       <tt>toIndex &gt; a.length</tt>
	   */
	  public static void sort(short[] a, int fromIndex, int toIndex) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  sort1(a, fromIndex, toIndex-fromIndex);
	  }

	  /**
	   * Sorts the specified array of chars into ascending numerical order.
	   * The sorting algorithm is a tuned quicksort, adapted from Jon
	   * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	   * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	   * 1993).  This algorithm offers n*log(n) performance on many data sets
	   * that cause other quicksorts to degrade to quadratic performance.
	   *
	   * @param a the array to be sorted
	   */
	  public static void sort(char[] a) {
		  sort1(a, 0, a.length);
	  }

	  /**
	   * Sorts the specified range of the specified array of chars into
	   * ascending numerical order.  The range to be sorted extends from index
	   * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
	   * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)<p>
	   *
	   * The sorting algorithm is a tuned quicksort, adapted from Jon
	   * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	   * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	   * 1993).  This algorithm offers n*log(n) performance on many data sets
	   * that cause other quicksorts to degrade to quadratic performance.
	   *
	   * @param a the array to be sorted
	   * @param fromIndex the index of the first element (inclusive) to be
	   *        sorted
	   * @param toIndex the index of the last element (exclusive) to be sorted
	   * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	   * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	   *	       <tt>toIndex &gt; a.length</tt>
	   */
	  public static void sort(char[] a, int fromIndex, int toIndex) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  sort1(a, fromIndex, toIndex-fromIndex);
	  }

	  /**
	   * Sorts the specified array of bytes into ascending numerical order.
	   * The sorting algorithm is a tuned quicksort, adapted from Jon
	   * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	   * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	   * 1993).  This algorithm offers n*log(n) performance on many data sets
	   * that cause other quicksorts to degrade to quadratic performance.
	   *
	   * @param a the array to be sorted
	   */
	  public static void sort(byte[] a) {
		  sort1(a, 0, a.length);
	  }

	  /**
	   * Sorts the specified range of the specified array of bytes into
	   * ascending numerical order.  The range to be sorted extends from index
	   * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
	   * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)<p>
	   *
	   * The sorting algorithm is a tuned quicksort, adapted from Jon
	   * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	   * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	   * 1993).  This algorithm offers n*log(n) performance on many data sets
	   * that cause other quicksorts to degrade to quadratic performance.
	   *
	   * @param a the array to be sorted
	   * @param fromIndex the index of the first element (inclusive) to be
	   *        sorted
	   * @param toIndex the index of the last element (exclusive) to be sorted
	   * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	   * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	   *	       <tt>toIndex &gt; a.length</tt>
	   */
	  public static void sort(byte[] a, int fromIndex, int toIndex) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  sort1(a, fromIndex, toIndex-fromIndex);
	  }

	  /**
	   * Sorts the specified array of doubles into ascending numerical order.
	   * <p>
	   * The <code>&lt;</code> relation does not provide a total order on
	   * all floating-point values; although they are distinct numbers
	   * <code>-0.0 == 0.0</code> is <code>true</code> and a NaN value
	   * compares neither less than, greater than, nor equal to any
	   * floating-point value, even itself.  To allow the sort to
	   * proceed, instead of using the <code>&lt;</code> relation to
	   * determine ascending numerical order, this method uses the total
	   * order imposed by {@link Double#compareTo}.  This ordering
	   * differs from the <code>&lt;</code> relation in that
	   * <code>-0.0</code> is treated as less than <code>0.0</code> and
	   * NaN is considered greater than any other floating-point value.
	   * For the purposes of sorting, all NaN values are considered
	   * equivalent and equal.
	   * <p>
	   * The sorting algorithm is a tuned quicksort, adapted from Jon
	   * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	   * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	   * 1993).  This algorithm offers n*log(n) performance on many data sets
	   * that cause other quicksorts to degrade to quadratic performance.
	   *
	   * @param a the array to be sorted
	   */
	  public static void sort(double[] a) {
		  sort2(a, 0, a.length);
	  }

	  /**
	   * Sorts the specified range of the specified array of doubles into
	   * ascending numerical order.  The range to be sorted extends from index
	   * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
	   * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)
	   * <p>
	   * The <code>&lt;</code> relation does not provide a total order on
	   * all floating-point values; although they are distinct numbers
	   * <code>-0.0 == 0.0</code> is <code>true</code> and a NaN value
	   * compares neither less than, greater than, nor equal to any
	   * floating-point value, even itself.  To allow the sort to
	   * proceed, instead of using the <code>&lt;</code> relation to
	   * determine ascending numerical order, this method uses the total
	   * order imposed by {@link Double#compareTo}.  This ordering
	   * differs from the <code>&lt;</code> relation in that
	   * <code>-0.0</code> is treated as less than <code>0.0</code> and
	   * NaN is considered greater than any other floating-point value.
	   * For the purposes of sorting, all NaN values are considered
	   * equivalent and equal.
	   * <p>
	   * The sorting algorithm is a tuned quicksort, adapted from Jon
	   * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	   * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	   * 1993).  This algorithm offers n*log(n) performance on many data sets
	   * that cause other quicksorts to degrade to quadratic performance.
	   *
	   * @param a the array to be sorted
	   * @param fromIndex the index of the first element (inclusive) to be
	   *        sorted
	   * @param toIndex the index of the last element (exclusive) to be sorted
	   * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	   * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	   *	       <tt>toIndex &gt; a.length</tt>
	   */
	  public static void sort(double[] a, int fromIndex, int toIndex) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  sort2(a, fromIndex, toIndex);
	  }

	  /**
	   * Sorts the specified array of floats into ascending numerical order.
	   * <p>
	   * The <code>&lt;</code> relation does not provide a total order on
	   * all floating-point values; although they are distinct numbers
	   * <code>-0.0f == 0.0f</code> is <code>true</code> and a NaN value
	   * compares neither less than, greater than, nor equal to any
	   * floating-point value, even itself.  To allow the sort to
	   * proceed, instead of using the <code>&lt;</code> relation to
	   * determine ascending numerical order, this method uses the total
	   * order imposed by {@link Float#compareTo}.  This ordering
	   * differs from the <code>&lt;</code> relation in that
	   * <code>-0.0f</code> is treated as less than <code>0.0f</code> and
	   * NaN is considered greater than any other floating-point value.
	   * For the purposes of sorting, all NaN values are considered
	   * equivalent and equal.
	   * <p>
	   * The sorting algorithm is a tuned quicksort, adapted from Jon
	   * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	   * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	   * 1993).  This algorithm offers n*log(n) performance on many data sets
	   * that cause other quicksorts to degrade to quadratic performance.
	   *
	   * @param a the array to be sorted
	   */
	  public static void sort(float[] a) {
		  sort2(a, 0, a.length);
	  }

	  /**
	   * Sorts the specified range of the specified array of floats into
	   * ascending numerical order.  The range to be sorted extends from index
	   * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
	   * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)
	   * <p>
	   * The <code>&lt;</code> relation does not provide a total order on
	   * all floating-point values; although they are distinct numbers
	   * <code>-0.0f == 0.0f</code> is <code>true</code> and a NaN value
	   * compares neither less than, greater than, nor equal to any
	   * floating-point value, even itself.  To allow the sort to
	   * proceed, instead of using the <code>&lt;</code> relation to
	   * determine ascending numerical order, this method uses the total
	   * order imposed by {@link Float#compareTo}.  This ordering
	   * differs from the <code>&lt;</code> relation in that
	   * <code>-0.0f</code> is treated as less than <code>0.0f</code> and
	   * NaN is considered greater than any other floating-point value.
	   * For the purposes of sorting, all NaN values are considered
	   * equivalent and equal.
	   * <p>
	   * The sorting algorithm is a tuned quicksort, adapted from Jon
	   * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	   * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	   * 1993).  This algorithm offers n*log(n) performance on many data sets
	   * that cause other quicksorts to degrade to quadratic performance.
	   *
	   * @param a the array to be sorted
	   * @param fromIndex the index of the first element (inclusive) to be
	   *        sorted
	   * @param toIndex the index of the last element (exclusive) to be sorted
	   * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	   * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	   *	       <tt>toIndex &gt; a.length</tt>
	   */
	  public static void sort(float[] a, int fromIndex, int toIndex) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  sort2(a, fromIndex, toIndex);
	  }

	  private static void sort2(double a[], int fromIndex, int toIndex) {
		  final long NEG_ZERO_BITS = Double.doubleToLongBits(-0.0d);
		  /*
		   * The sort is done in three phases to avoid the expense of using
		   * NaN and -0.0 aware comparisons during the main sort.
		   */

		  /*
		   * Preprocessing phase:  Move any NaN's to end of array, count the
		   * number of -0.0's, and turn them into 0.0's.
		   */
		  int numNegZeros = 0;
		  int i = fromIndex, n = toIndex;
		  while(i < n) {
			  if (a[i] != a[i]) {
				  double swap = a[i];
				  a[i] = a[--n];
				  a[n] = swap;
			  } else {
				  if (a[i]==0 && Double.doubleToLongBits(a[i])==NEG_ZERO_BITS) {
					  a[i] = 0.0d;
					  numNegZeros++;
				  }
				  i++;
			  }
		  }

		  // Main sort phase: quicksort everything but the NaN's
		  sort1(a, fromIndex, n-fromIndex);

		  // Postprocessing phase: change 0.0's to -0.0's as required
		  if (numNegZeros != 0) {
			  int j = binarySearch0(a, fromIndex, n, 0.0d); // posn of ANY zero
			  do {
				  j--;
			  } while (j>=0 && a[j]==0.0d);

			  // j is now one less than the index of the FIRST zero
			  for (int k=0; k<numNegZeros; k++)
				  a[++j] = -0.0d;
		  }
	  }


	  private static void sort2(float a[], int fromIndex, int toIndex) {
		  final int NEG_ZERO_BITS = Float.floatToIntBits(-0.0f);
		  /*
		   * The sort is done in three phases to avoid the expense of using
		   * NaN and -0.0 aware comparisons during the main sort.
		   */

		  /*
		   * Preprocessing phase:  Move any NaN's to end of array, count the
		   * number of -0.0's, and turn them into 0.0's.
		   */
		  int numNegZeros = 0;
		  int i = fromIndex, n = toIndex;
		  while(i < n) {
			  if (a[i] != a[i]) {
				  float swap = a[i];
				  a[i] = a[--n];
				  a[n] = swap;
			  } else {
				  if (a[i]==0 && Float.floatToIntBits(a[i])==NEG_ZERO_BITS) {
					  a[i] = 0.0f;
					  numNegZeros++;
				  }
				  i++;
			  }
		  }

		  // Main sort phase: quicksort everything but the NaN's
		  sort1(a, fromIndex, n-fromIndex);

		  // Postprocessing phase: change 0.0's to -0.0's as required
		  if (numNegZeros != 0) {
			  int j = binarySearch0(a, fromIndex, n, 0.0f); // posn of ANY zero
			  do {
				  j--;
			  } while (j>=0 && a[j]==0.0f);

			  // j is now one less than the index of the FIRST zero
			  for (int k=0; k<numNegZeros; k++)
				  a[++j] = -0.0f;
		  }
	  }


	  /*
	   * The code for each of the seven primitive types is largely identical.
	   * C'est la vie.
	   */

	  /**
	   * Sorts the specified sub-array of longs into ascending order.
	   */
	  private static void sort1(long x[], int off, int len) {
		  // Insertion sort on smallest arrays
		  if (len < 7) {
			  for (int i=off; i<len+off; i++)
				  for (int j=i; j>off && x[j-1]>x[j]; j--)
					  swap(x, j, j-1);
			  return;
		  }

		  // Choose a partition element, v
		  int m = off + (len >> 1);       // Small arrays, middle element
		  if (len > 7) {
			  int l = off;
			  int n = off + len - 1;
			  if (len > 40) {        // Big arrays, pseudomedian of 9
				  int s = len/8;
				  l = med3(x, l,     l+s, l+2*s);
				  m = med3(x, m-s,   m,   m+s);
				  n = med3(x, n-2*s, n-s, n);
			  }
			  m = med3(x, l, m, n); // Mid-size, med of 3
		  }
		  long v = x[m];

		  // Establish Invariant: v* (<v)* (>v)* v*
		  int a = off, b = a, c = off + len - 1, d = c;
		  while(true) {
			  while (b <= c && x[b] <= v) {
				  if (x[b] == v)
					  swap(x, a++, b);
				  b++;
			  }
			  while (c >= b && x[c] >= v) {
				  if (x[c] == v)
					  swap(x, c, d--);
				  c--;
			  }
			  if (b > c)
				  break;
			  swap(x, b++, c--);
		  }

		  // Swap partition elements back to middle
		  int s, n = off + len;
		  s = Math.min(a-off, b-a  );  vecswap(x, off, b-s, s);
		  s = Math.min(d-c,   n-d-1);  vecswap(x, b,   n-s, s);

		  // Recursively sort non-partition-elements
		  if ((s = b-a) > 1)
			  sort1(x, off, s);
		  if ((s = d-c) > 1)
			  sort1(x, n-s, s);
	  }

	  /**
	   * Swaps x[a] with x[b].
	   */
	  private static void swap(long x[], int a, int b) {
		  long t = x[a];
		  x[a] = x[b];
		  x[b] = t;
	  }

	  /**
	   * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	   */
	  private static void vecswap(long x[], int a, int b, int n) {
		  for (int i=0; i<n; i++, a++, b++)
			  swap(x, a, b);
	  }

	  /**
	   * Returns the index of the median of the three indexed longs.
	   */
	  private static int med3(long x[], int a, int b, int c) {
		  return (x[a] < x[b] ?
				  (x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
					  (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
	  }

	  /**
	   * Sorts the specified sub-array of integers into ascending order.
	   */
	  private static void sort1(int x[], int off, int len) {
		  // Insertion sort on smallest arrays
		  if (len < 7) {
			  for (int i=off; i<len+off; i++)
				  for (int j=i; j>off && x[j-1]>x[j]; j--)
					  swap(x, j, j-1);
			  return;
		  }

		  // Choose a partition element, v
		  int m = off + (len >> 1);       // Small arrays, middle element
		  if (len > 7) {
			  int l = off;
			  int n = off + len - 1;
			  if (len > 40) {        // Big arrays, pseudomedian of 9
				  int s = len/8;
				  l = med3(x, l,     l+s, l+2*s);
				  m = med3(x, m-s,   m,   m+s);
				  n = med3(x, n-2*s, n-s, n);
			  }
			  m = med3(x, l, m, n); // Mid-size, med of 3
		  }
		  int v = x[m];

		  // Establish Invariant: v* (<v)* (>v)* v*
		  int a = off, b = a, c = off + len - 1, d = c;
		  while(true) {
			  while (b <= c && x[b] <= v) {
				  if (x[b] == v)
					  swap(x, a++, b);
				  b++;
			  }
			  while (c >= b && x[c] >= v) {
				  if (x[c] == v)
					  swap(x, c, d--);
				  c--;
			  }
			  if (b > c)
				  break;
			  swap(x, b++, c--);
		  }

		  // Swap partition elements back to middle
		  int s, n = off + len;
		  s = Math.min(a-off, b-a  );  vecswap(x, off, b-s, s);
		  s = Math.min(d-c,   n-d-1);  vecswap(x, b,   n-s, s);

		  // Recursively sort non-partition-elements
		  if ((s = b-a) > 1)
			  sort1(x, off, s);
		  if ((s = d-c) > 1)
			  sort1(x, n-s, s);
	  }

	  /**
	   * Swaps x[a] with x[b].
	   */
	  private static void swap(int x[], int a, int b) {
		  int t = x[a];
		  x[a] = x[b];
		  x[b] = t;
	  }

	  /**
	   * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	   */
	  private static void vecswap(int x[], int a, int b, int n) {
		  for (int i=0; i<n; i++, a++, b++)
			  swap(x, a, b);
	  }

	  /**
	   * Returns the index of the median of the three indexed integers.
	   */
	  private static int med3(int x[], int a, int b, int c) {
		  return (x[a] < x[b] ?
				  (x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
					  (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
	  }

	  /**
	   * Sorts the specified sub-array of shorts into ascending order.
	   */
	  private static void sort1(short x[], int off, int len) {
		  // Insertion sort on smallest arrays
		  if (len < 7) {
			  for (int i=off; i<len+off; i++)
				  for (int j=i; j>off && x[j-1]>x[j]; j--)
					  swap(x, j, j-1);
			  return;
		  }

		  // Choose a partition element, v
		  int m = off + (len >> 1);       // Small arrays, middle element
		  if (len > 7) {
			  int l = off;
			  int n = off + len - 1;
			  if (len > 40) {        // Big arrays, pseudomedian of 9
				  int s = len/8;
				  l = med3(x, l,     l+s, l+2*s);
				  m = med3(x, m-s,   m,   m+s);
				  n = med3(x, n-2*s, n-s, n);
			  }
			  m = med3(x, l, m, n); // Mid-size, med of 3
		  }
		  short v = x[m];

		  // Establish Invariant: v* (<v)* (>v)* v*
		  int a = off, b = a, c = off + len - 1, d = c;
		  while(true) {
			  while (b <= c && x[b] <= v) {
				  if (x[b] == v)
					  swap(x, a++, b);
				  b++;
			  }
			  while (c >= b && x[c] >= v) {
				  if (x[c] == v)
					  swap(x, c, d--);
				  c--;
			  }
			  if (b > c)
				  break;
			  swap(x, b++, c--);
		  }

		  // Swap partition elements back to middle
		  int s, n = off + len;
		  s = Math.min(a-off, b-a  );  vecswap(x, off, b-s, s);
		  s = Math.min(d-c,   n-d-1);  vecswap(x, b,   n-s, s);

		  // Recursively sort non-partition-elements
		  if ((s = b-a) > 1)
			  sort1(x, off, s);
		  if ((s = d-c) > 1)
			  sort1(x, n-s, s);
	  }

	  /**
	   * Swaps x[a] with x[b].
	   */
	  private static void swap(short x[], int a, int b) {
		  short t = x[a];
		  x[a] = x[b];
		  x[b] = t;
	  }

	  /**
	   * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	   */
	  private static void vecswap(short x[], int a, int b, int n) {
		  for (int i=0; i<n; i++, a++, b++)
			  swap(x, a, b);
	  }

	  /**
	   * Returns the index of the median of the three indexed shorts.
	   */
	  private static int med3(short x[], int a, int b, int c) {
		  return (x[a] < x[b] ?
				  (x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
					  (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
	  }


	  /**
	   * Sorts the specified sub-array of chars into ascending order.
	   */
	  private static void sort1(char x[], int off, int len) {
		  // Insertion sort on smallest arrays
		  if (len < 7) {
			  for (int i=off; i<len+off; i++)
				  for (int j=i; j>off && x[j-1]>x[j]; j--)
					  swap(x, j, j-1);
			  return;
		  }

		  // Choose a partition element, v
		  int m = off + (len >> 1);       // Small arrays, middle element
		  if (len > 7) {
			  int l = off;
			  int n = off + len - 1;
			  if (len > 40) {        // Big arrays, pseudomedian of 9
				  int s = len/8;
				  l = med3(x, l,     l+s, l+2*s);
				  m = med3(x, m-s,   m,   m+s);
				  n = med3(x, n-2*s, n-s, n);
			  }
			  m = med3(x, l, m, n); // Mid-size, med of 3
		  }
		  char v = x[m];

		  // Establish Invariant: v* (<v)* (>v)* v*
		  int a = off, b = a, c = off + len - 1, d = c;
		  while(true) {
			  while (b <= c && x[b] <= v) {
				  if (x[b] == v)
					  swap(x, a++, b);
				  b++;
			  }
			  while (c >= b && x[c] >= v) {
				  if (x[c] == v)
					  swap(x, c, d--);
				  c--;
			  }
			  if (b > c)
				  break;
			  swap(x, b++, c--);
		  }

		  // Swap partition elements back to middle
		  int s, n = off + len;
		  s = Math.min(a-off, b-a  );  vecswap(x, off, b-s, s);
		  s = Math.min(d-c,   n-d-1);  vecswap(x, b,   n-s, s);

		  // Recursively sort non-partition-elements
		  if ((s = b-a) > 1)
			  sort1(x, off, s);
		  if ((s = d-c) > 1)
			  sort1(x, n-s, s);
	  }

	  /**
	   * Swaps x[a] with x[b].
	   */
	  private static void swap(char x[], int a, int b) {
		  char t = x[a];
		  x[a] = x[b];
		  x[b] = t;
	  }

	  /**
	   * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	   */
	  private static void vecswap(char x[], int a, int b, int n) {
		  for (int i=0; i<n; i++, a++, b++)
			  swap(x, a, b);
	  }

	  /**
	   * Returns the index of the median of the three indexed chars.
	   */
	  private static int med3(char x[], int a, int b, int c) {
		  return (x[a] < x[b] ?
				  (x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
					  (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
	  }


	  /**
	   * Sorts the specified sub-array of bytes into ascending order.
	   */
	  private static void sort1(byte x[], int off, int len) {
		  // Insertion sort on smallest arrays
		  if (len < 7) {
			  for (int i=off; i<len+off; i++)
				  for (int j=i; j>off && x[j-1]>x[j]; j--)
					  swap(x, j, j-1);
			  return;
		  }

		  // Choose a partition element, v
		  int m = off + (len >> 1);       // Small arrays, middle element
		  if (len > 7) {
			  int l = off;
			  int n = off + len - 1;
			  if (len > 40) {        // Big arrays, pseudomedian of 9
				  int s = len/8;
				  l = med3(x, l,     l+s, l+2*s);
				  m = med3(x, m-s,   m,   m+s);
				  n = med3(x, n-2*s, n-s, n);
			  }
			  m = med3(x, l, m, n); // Mid-size, med of 3
		  }
		  byte v = x[m];

		  // Establish Invariant: v* (<v)* (>v)* v*
		  int a = off, b = a, c = off + len - 1, d = c;
		  while(true) {
			  while (b <= c && x[b] <= v) {
				  if (x[b] == v)
					  swap(x, a++, b);
				  b++;
			  }
			  while (c >= b && x[c] >= v) {
				  if (x[c] == v)
					  swap(x, c, d--);
				  c--;
			  }
			  if (b > c)
				  break;
			  swap(x, b++, c--);
		  }

		  // Swap partition elements back to middle
		  int s, n = off + len;
		  s = Math.min(a-off, b-a  );  vecswap(x, off, b-s, s);
		  s = Math.min(d-c,   n-d-1);  vecswap(x, b,   n-s, s);

		  // Recursively sort non-partition-elements
		  if ((s = b-a) > 1)
			  sort1(x, off, s);
		  if ((s = d-c) > 1)
			  sort1(x, n-s, s);
	  }

	  /**
	   * Swaps x[a] with x[b].
	   */
	  private static void swap(byte x[], int a, int b) {
		  byte t = x[a];
		  x[a] = x[b];
		  x[b] = t;
	  }

	  /**
	   * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	   */
	  private static void vecswap(byte x[], int a, int b, int n) {
		  for (int i=0; i<n; i++, a++, b++)
			  swap(x, a, b);
	  }

	  /**
	   * Returns the index of the median of the three indexed bytes.
	   */
	  private static int med3(byte x[], int a, int b, int c) {
		  return (x[a] < x[b] ?
				  (x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
					  (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
	  }


	  /**
	   * Sorts the specified sub-array of doubles into ascending order.
	   */
	  private static void sort1(double x[], int off, int len) {
		  // Insertion sort on smallest arrays
		  if (len < 7) {
			  for (int i=off; i<len+off; i++)
				  for (int j=i; j>off && x[j-1]>x[j]; j--)
					  swap(x, j, j-1);
			  return;
		  }

		  // Choose a partition element, v
		  int m = off + (len >> 1);       // Small arrays, middle element
		  if (len > 7) {
			  int l = off;
			  int n = off + len - 1;
			  if (len > 40) {        // Big arrays, pseudomedian of 9
				  int s = len/8;
				  l = med3(x, l,     l+s, l+2*s);
				  m = med3(x, m-s,   m,   m+s);
				  n = med3(x, n-2*s, n-s, n);
			  }
			  m = med3(x, l, m, n); // Mid-size, med of 3
		  }
		  double v = x[m];

		  // Establish Invariant: v* (<v)* (>v)* v*
		  int a = off, b = a, c = off + len - 1, d = c;
		  while(true) {
			  while (b <= c && x[b] <= v) {
				  if (x[b] == v)
					  swap(x, a++, b);
				  b++;
			  }
			  while (c >= b && x[c] >= v) {
				  if (x[c] == v)
					  swap(x, c, d--);
				  c--;
			  }
			  if (b > c)
				  break;
			  swap(x, b++, c--);
		  }

		  // Swap partition elements back to middle
		  int s, n = off + len;
		  s = Math.min(a-off, b-a  );  vecswap(x, off, b-s, s);
		  s = Math.min(d-c,   n-d-1);  vecswap(x, b,   n-s, s);

		  // Recursively sort non-partition-elements
		  if ((s = b-a) > 1)
			  sort1(x, off, s);
		  if ((s = d-c) > 1)
			  sort1(x, n-s, s);
	  }

	  /**
	   * Swaps x[a] with x[b].
	   */
	  private static void swap(double x[], int a, int b) {
		  double t = x[a];
		  x[a] = x[b];
		  x[b] = t;
	  }

	  /**
	   * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	   */
	  private static void vecswap(double x[], int a, int b, int n) {
		  for (int i=0; i<n; i++, a++, b++)
			  swap(x, a, b);
	  }

	  /**
	   * Returns the index of the median of the three indexed doubles.
	   */
	  private static int med3(double x[], int a, int b, int c) {
		  return (x[a] < x[b] ?
				  (x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
					  (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
	  }


	  /**
	   * Sorts the specified sub-array of floats into ascending order.
	   */
	  private static void sort1(float x[], int off, int len) {
		  // Insertion sort on smallest arrays
		  if (len < 7) {
			  for (int i=off; i<len+off; i++)
				  for (int j=i; j>off && x[j-1]>x[j]; j--)
					  swap(x, j, j-1);
			  return;
		  }

		  // Choose a partition element, v
		  int m = off + (len >> 1);       // Small arrays, middle element
		  if (len > 7) {
			  int l = off;
			  int n = off + len - 1;
			  if (len > 40) {        // Big arrays, pseudomedian of 9
				  int s = len/8;
				  l = med3(x, l,     l+s, l+2*s);
				  m = med3(x, m-s,   m,   m+s);
				  n = med3(x, n-2*s, n-s, n);
			  }
			  m = med3(x, l, m, n); // Mid-size, med of 3
		  }
		  float v = x[m];

		  // Establish Invariant: v* (<v)* (>v)* v*
		  int a = off, b = a, c = off + len - 1, d = c;
		  while(true) {
			  while (b <= c && x[b] <= v) {
				  if (x[b] == v)
					  swap(x, a++, b);
				  b++;
			  }
			  while (c >= b && x[c] >= v) {
				  if (x[c] == v)
					  swap(x, c, d--);
				  c--;
			  }
			  if (b > c)
				  break;
			  swap(x, b++, c--);
		  }

		  // Swap partition elements back to middle
		  int s, n = off + len;
		  s = Math.min(a-off, b-a  );  vecswap(x, off, b-s, s);
		  s = Math.min(d-c,   n-d-1);  vecswap(x, b,   n-s, s);

		  // Recursively sort non-partition-elements
		  if ((s = b-a) > 1)
			  sort1(x, off, s);
		  if ((s = d-c) > 1)
			  sort1(x, n-s, s);
	  }

	  /**
	   * Swaps x[a] with x[b].
	   */
	  private static void swap(float x[], int a, int b) {
		  float t = x[a];
		  x[a] = x[b];
		  x[b] = t;
	  }

	  /**
	   * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	   */
	  private static void vecswap(float x[], int a, int b, int n) {
		  for (int i=0; i<n; i++, a++, b++)
			  swap(x, a, b);
	  }

	  /**
	   * Returns the index of the median of the three indexed floats.
	   */
	  private static int med3(float x[], int a, int b, int c) {
		  return (x[a] < x[b] ?
				  (x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
					  (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
	  }


	  /**
	   * Sorts the specified array of objects into ascending order, according to
	   * the {@linkplain Comparable natural ordering}
	   * of its elements.  All elements in the array
	   * must implement the {@link Comparable} interface.  Furthermore, all
	   * elements in the array must be <i>mutually comparable</i> (that is,
	   * <tt>e1.compareTo(e2)</tt> must not throw a <tt>ClassCastException</tt>
	   * for any elements <tt>e1</tt> and <tt>e2</tt> in the array).<p>
	   *
	   * This sort is guaranteed to be <i>stable</i>:  equal elements will
	   * not be reordered as a result of the sort.<p>
	   *
	   * The sorting algorithm is a modified mergesort (in which the merge is
	   * omitted if the highest element in the low sublist is less than the
	   * lowest element in the high sublist).  This algorithm offers guaranteed
	   * n*log(n) performance.
	   *
	   * @param a the array to be sorted
	   * @throws  ClassCastException if the array contains elements that are not
	   *		<i>mutually comparable</i> (for example, strings and integers).
	   */
	  public static void sort(Object[] a) {
		  Object[] aux = (Object[])a.clone();
		  mergeSort(aux, a, 0, a.length, 0);
	  }

	  /**
	   * Sorts the specified range of the specified array of objects into
	   * ascending order, according to the
	   * {@linkplain Comparable natural ordering} of its
	   * elements.  The range to be sorted extends from index
	   * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
	   * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)  All
	   * elements in this range must implement the {@link Comparable}
	   * interface.  Furthermore, all elements in this range must be <i>mutually
	   * comparable</i> (that is, <tt>e1.compareTo(e2)</tt> must not throw a
	   * <tt>ClassCastException</tt> for any elements <tt>e1</tt> and
	   * <tt>e2</tt> in the array).<p>
	   *
	   * This sort is guaranteed to be <i>stable</i>:  equal elements will
	   * not be reordered as a result of the sort.<p>
	   *
	   * The sorting algorithm is a modified mergesort (in which the merge is
	   * omitted if the highest element in the low sublist is less than the
	   * lowest element in the high sublist).  This algorithm offers guaranteed
	   * n*log(n) performance.
	   *
	   * @param a the array to be sorted
	   * @param fromIndex the index of the first element (inclusive) to be
	   *        sorted
	   * @param toIndex the index of the last element (exclusive) to be sorted
	   * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	   * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	   *	       <tt>toIndex &gt; a.length</tt>
	   * @throws    ClassCastException if the array contains elements that are
	   *		  not <i>mutually comparable</i> (for example, strings and
	   *		  integers).
	   */
	  public static void sort(Object[] a, int fromIndex, int toIndex) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  Object[] aux = copyOfRange(a, fromIndex, toIndex);
		  mergeSort(aux, a, fromIndex, toIndex, -fromIndex);
	  }

	  /**
	   * Tuning parameter: list size at or below which insertion sort will be
	   * used in preference to mergesort or quicksort.
	   */
	  private static final int INSERTIONSORT_THRESHOLD = 7;

	  /**
	   * Src is the source array that starts at index 0
	   * Dest is the (possibly larger) array destination with a possible offset
	   * low is the index in dest to start sorting
	   * high is the end index in dest to end sorting
	   * off is the offset to generate corresponding low, high in src
	   */
	  private static void mergeSort(Object[] src,
			  Object[] dest,
			  int low,
			  int high,
			  int off) {
		  int length = high - low;

		  // Insertion sort on smallest arrays
		  if (length < INSERTIONSORT_THRESHOLD) {
			  for (int i=low; i<high; i++)
				  for (int j=i; j>low &&
				  ((Comparable) dest[j-1]).compareTo(dest[j])>0; j--)
					  swap(dest, j, j-1);
			  return;
		  }

		  // Recursively sort halves of dest into src
		  int destLow  = low;
		  int destHigh = high;
		  low  += off;
		  high += off;
		  int mid = (low + high) >>> 1;
		  mergeSort(dest, src, low, mid, -off);
		  mergeSort(dest, src, mid, high, -off);

		  // If list is already sorted, just copy from src to dest.  This is an
		  // optimization that results in faster sorts for nearly ordered lists.
		  if (((Comparable)src[mid-1]).compareTo(src[mid]) <= 0) {
			  System.arraycopy(src, low, dest, destLow, length);
			  return;
		  }

		  // Merge sorted halves (now in src) into dest
		  for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
			  if (q >= high || p < mid && ((Comparable)src[p]).compareTo(src[q])<=0)
				  dest[i] = src[p++];
			  else
				  dest[i] = src[q++];
		  }
	  }

	  /**
	   * Swaps x[a] with x[b].
	   */
	  private static void swap(Object[] x, int a, int b) {
		  Object t = x[a];
		  x[a] = x[b];
		  x[b] = t;
	  }

	  /**
	   * Sorts the specified array of objects according to the order induced by
	   * the specified comparator.  All elements in the array must be
	   * <i>mutually comparable</i> by the specified comparator (that is,
	   * <tt>c.compare(e1, e2)</tt> must not throw a <tt>ClassCastException</tt>
	   * for any elements <tt>e1</tt> and <tt>e2</tt> in the array).<p>
	   *
	   * This sort is guaranteed to be <i>stable</i>:  equal elements will
	   * not be reordered as a result of the sort.<p>
	   *
	   * The sorting algorithm is a modified mergesort (in which the merge is
	   * omitted if the highest element in the low sublist is less than the
	   * lowest element in the high sublist).  This algorithm offers guaranteed
	   * n*log(n) performance.
	   *
	   * @param a the array to be sorted
	   * @param c the comparator to determine the order of the array.  A
	   *        <tt>null</tt> value indicates that the elements'
	   *        {@linkplain Comparable natural ordering} should be used.
	   * @throws  ClassCastException if the array contains elements that are
	   *		not <i>mutually comparable</i> using the specified comparator.
	   */
	  public static <T> void sort(T[] a, Comparator<? super T> c) {
		  T[] aux = (T[])a.clone();
		  if (c==null)
			  mergeSort(aux, a, 0, a.length, 0);
		  else
			  mergeSort(aux, a, 0, a.length, 0, c);
	  }

	  /**
	   * Sorts the specified range of the specified array of objects according
	   * to the order induced by the specified comparator.  The range to be
	   * sorted extends from index <tt>fromIndex</tt>, inclusive, to index
	   * <tt>toIndex</tt>, exclusive.  (If <tt>fromIndex==toIndex</tt>, the
	   * range to be sorted is empty.)  All elements in the range must be
	   * <i>mutually comparable</i> by the specified comparator (that is,
	   * <tt>c.compare(e1, e2)</tt> must not throw a <tt>ClassCastException</tt>
	   * for any elements <tt>e1</tt> and <tt>e2</tt> in the range).<p>
	   *
	   * This sort is guaranteed to be <i>stable</i>:  equal elements will
	   * not be reordered as a result of the sort.<p>
	   *
	   * The sorting algorithm is a modified mergesort (in which the merge is
	   * omitted if the highest element in the low sublist is less than the
	   * lowest element in the high sublist).  This algorithm offers guaranteed
	   * n*log(n) performance.
	   *
	   * @param a the array to be sorted
	   * @param fromIndex the index of the first element (inclusive) to be
	   *        sorted
	   * @param toIndex the index of the last element (exclusive) to be sorted
	   * @param c the comparator to determine the order of the array.  A
	   *        <tt>null</tt> value indicates that the elements'
	   *        {@linkplain Comparable natural ordering} should be used.
	   * @throws ClassCastException if the array contains elements that are not
	   *	       <i>mutually comparable</i> using the specified comparator.
	   * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	   * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	   *	       <tt>toIndex &gt; a.length</tt>
	   */
	  public static <T> void sort(T[] a, int fromIndex, int toIndex,
			  Comparator<? super T> c) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  T[] aux = (T[])copyOfRange(a, fromIndex, toIndex);
		  if (c==null)
			  mergeSort(aux, a, fromIndex, toIndex, -fromIndex);
		  else
			  mergeSort(aux, a, fromIndex, toIndex, -fromIndex, c);
	  }

	  /**
	   * Src is the source array that starts at index 0
	   * Dest is the (possibly larger) array destination with a possible offset
	   * low is the index in dest to start sorting
	   * high is the end index in dest to end sorting
	   * off is the offset into src corresponding to low in dest
	   */
	  private static void mergeSort(Object[] src,
			  Object[] dest,
			  int low, int high, int off,
			  Comparator c) {
		  int length = high - low;

		  // Insertion sort on smallest arrays
		  if (length < INSERTIONSORT_THRESHOLD) {
			  for (int i=low; i<high; i++)
				  for (int j=i; j>low && c.compare(dest[j-1], dest[j])>0; j--)
					  swap(dest, j, j-1);
			  return;
		  }

		  // Recursively sort halves of dest into src
		  int destLow  = low;
		  int destHigh = high;
		  low  += off;
		  high += off;
		  int mid = (low + high) >>> 1;
			  mergeSort(dest, src, low, mid, -off, c);
			  mergeSort(dest, src, mid, high, -off, c);

			  // If list is already sorted, just copy from src to dest.  This is an
			  // optimization that results in faster sorts for nearly ordered lists.
			  if (c.compare(src[mid-1], src[mid]) <= 0) {
				  System.arraycopy(src, low, dest, destLow, length);
				  return;
			  }

			  // Merge sorted halves (now in src) into dest
			  for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
				  if (q >= high || p < mid && c.compare(src[p], src[q]) <= 0)
					  dest[i] = src[p++];
				  else
					  dest[i] = src[q++];
			  }
	  }

	  /**
	   * Check that fromIndex and toIndex are in range, and throw an
	   * appropriate exception if they aren't.
	   */
	  private static void rangeCheck(int arrayLen, int fromIndex, int toIndex) {
		  if (fromIndex > toIndex)
			  throw new IllegalArgumentException("fromIndex(" + fromIndex +
					  ") > toIndex(" + toIndex+")");
		  if (fromIndex < 0)
			  throw new ArrayIndexOutOfBoundsException(fromIndex);
		  if (toIndex > arrayLen)
			  throw new ArrayIndexOutOfBoundsException(toIndex);
	  }

	  // Searching

	  /**
	   * Searches the specified array of longs for the specified value using the
	   * binary search algorithm.  The array must be sorted (as
	   * by the {@link #sort(long[])} method) prior to making this call.  If it
	   * is not sorted, the results are undefined.  If the array contains
	   * multiple elements with the specified value, there is no guarantee which
	   * one will be found.
	   *
	   * @param a the array to be searched
	   * @param key the value to be searched for
	   * @return index of the search key, if it is contained in the array;
	   *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
	   *	       <i>insertion point</i> is defined as the point at which the
	   *	       key would be inserted into the array: the index of the first
	   *	       element greater than the key, or <tt>a.length</tt> if all
	   *	       elements in the array are less than the specified key.  Note
	   *	       that this guarantees that the return value will be &gt;= 0 if
	   *	       and only if the key is found.
	   */
	  public static int binarySearch(long[] a, long key) {
		  return binarySearch0(a, 0, a.length, key);
	  }

	  /**
	   * Searches a range of
	   * the specified array of longs for the specified value using the
	   * binary search algorithm.
	   * The range must be sorted (as
	   * by the {@link #sort(long[], int, int)} method)
	   * prior to making this call.  If it
	   * is not sorted, the results are undefined.  If the range contains
	   * multiple elements with the specified value, there is no guarantee which
	   * one will be found.
	   *
	   * @param a the array to be searched
	   * @param fromIndex the index of the first element (inclusive) to be
	   *		searched
	   * @param toIndex the index of the last element (exclusive) to be searched
	   * @param key the value to be searched for
	   * @return index of the search key, if it is contained in the array
	   *	       within the specified range;
	   *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
	   *	       <i>insertion point</i> is defined as the point at which the
	   *	       key would be inserted into the array: the index of the first
	   *	       element in the range greater than the key,
	   *	       or <tt>toIndex</tt> if all
	   *	       elements in the range are less than the specified key.  Note
	   *	       that this guarantees that the return value will be &gt;= 0 if
	   *	       and only if the key is found.
	   * @throws IllegalArgumentException
	   *	       if {@code fromIndex > toIndex}
	   * @throws ArrayIndexOutOfBoundsException
	   *	       if {@code fromIndex < 0 or toIndex > a.length}
	   * @since 1.6
	   */
	  public static int binarySearch(long[] a, int fromIndex, int toIndex,
			  long key) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  return binarySearch0(a, fromIndex, toIndex, key);
	  }

	  // Like public version, but without range checks.
	  private static int binarySearch0(long[] a, int fromIndex, int toIndex,
			  long key) {
		  int low = fromIndex;
		  int high = toIndex - 1;

		  while (low <= high) {
			  int mid = (low + high) >>> 1;
				  long midVal = a[mid];

				  if (midVal < key)
					  low = mid + 1;
				  else if (midVal > key)
					  high = mid - 1;
				  else
					  return mid; // key found
		  }
		  return -(low + 1);  // key not found.
	  }

	  /**
	   * Searches the specified array of ints for the specified value using the
	   * binary search algorithm.  The array must be sorted (as
	   * by the {@link #sort(int[])} method) prior to making this call.  If it
	   * is not sorted, the results are undefined.  If the array contains
	   * multiple elements with the specified value, there is no guarantee which
	   * one will be found.
	   *
	   * @param a the array to be searched
	   * @param key the value to be searched for
	   * @return index of the search key, if it is contained in the array;
	   *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
	   *	       <i>insertion point</i> is defined as the point at which the
	   *	       key would be inserted into the array: the index of the first
	   *	       element greater than the key, or <tt>a.length</tt> if all
	   *	       elements in the array are less than the specified key.  Note
	   *	       that this guarantees that the return value will be &gt;= 0 if
	   *	       and only if the key is found.
	   */
	  public static int binarySearch(int[] a, int key) {
		  return binarySearch0(a, 0, a.length, key);
	  }

	  /**
	   * Searches a range of
	   * the specified array of ints for the specified value using the
	   * binary search algorithm.
	   * The range must be sorted (as
	   * by the {@link #sort(int[], int, int)} method)
	   * prior to making this call.  If it
	   * is not sorted, the results are undefined.  If the range contains
	   * multiple elements with the specified value, there is no guarantee which
	   * one will be found.
	   *
	   * @param a the array to be searched
	   * @param fromIndex the index of the first element (inclusive) to be
	   *		searched
	   * @param toIndex the index of the last element (exclusive) to be searched
	   * @param key the value to be searched for
	   * @return index of the search key, if it is contained in the array
	   *	       within the specified range;
	   *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
	   *	       <i>insertion point</i> is defined as the point at which the
	   *	       key would be inserted into the array: the index of the first
	   *	       element in the range greater than the key,
	   *	       or <tt>toIndex</tt> if all
	   *	       elements in the range are less than the specified key.  Note
	   *	       that this guarantees that the return value will be &gt;= 0 if
	   *	       and only if the key is found.
	   * @throws IllegalArgumentException
	   *	       if {@code fromIndex > toIndex}
	   * @throws ArrayIndexOutOfBoundsException
	   *	       if {@code fromIndex < 0 or toIndex > a.length}
	   * @since 1.6
	   */
	  public static int binarySearch(int[] a, int fromIndex, int toIndex,
			  int key) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  return binarySearch0(a, fromIndex, toIndex, key);
	  }

	  // Like public version, but without range checks.
	  private static int binarySearch0(int[] a, int fromIndex, int toIndex,
			  int key) {
		  int low = fromIndex;
		  int high = toIndex - 1;

		  while (low <= high) {
			  int mid = (low + high) >>> 1;
		  int midVal = a[mid];

		  if (midVal < key)
			  low = mid + 1;
		  else if (midVal > key)
			  high = mid - 1;
		  else
			  return mid; // key found
		  }
		  return -(low + 1);  // key not found.
	  }

	  /**
	   * Searches the specified array of shorts for the specified value using
	   * the binary search algorithm.  The array must be sorted
	   * (as by the {@link #sort(short[])} method) prior to making this call.  If
	   * it is not sorted, the results are undefined.  If the array contains
	   * multiple elements with the specified value, there is no guarantee which
	   * one will be found.
	   *
	   * @param a the array to be searched
	   * @param key the value to be searched for
	   * @return index of the search key, if it is contained in the array;
	   *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
	   *	       <i>insertion point</i> is defined as the point at which the
	   *	       key would be inserted into the array: the index of the first
	   *	       element greater than the key, or <tt>a.length</tt> if all
	   *	       elements in the array are less than the specified key.  Note
	   *	       that this guarantees that the return value will be &gt;= 0 if
	   *	       and only if the key is found.
	   */
	  public static int binarySearch(short[] a, short key) {
		  return binarySearch0(a, 0, a.length, key);
	  }

	  /**
	   * Searches a range of
	   * the specified array of shorts for the specified value using
	   * the binary search algorithm.
	   * The range must be sorted
	   * (as by the {@link #sort(short[], int, int)} method)
	   * prior to making this call.  If
	   * it is not sorted, the results are undefined.  If the range contains
	   * multiple elements with the specified value, there is no guarantee which
	   * one will be found.
	   *
	   * @param a the array to be searched
	   * @param fromIndex the index of the first element (inclusive) to be
	   *		searched
	   * @param toIndex the index of the last element (exclusive) to be searched
	   * @param key the value to be searched for
	   * @return index of the search key, if it is contained in the array
	   *	       within the specified range;
	   *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
	   *	       <i>insertion point</i> is defined as the point at which the
	   *	       key would be inserted into the array: the index of the first
	   *	       element in the range greater than the key,
	   *	       or <tt>toIndex</tt> if all
	   *	       elements in the range are less than the specified key.  Note
	   *	       that this guarantees that the return value will be &gt;= 0 if
	   *	       and only if the key is found.
	   * @throws IllegalArgumentException
	   *	       if {@code fromIndex > toIndex}
	   * @throws ArrayIndexOutOfBoundsException
	   *	       if {@code fromIndex < 0 or toIndex > a.length}
	   * @since 1.6
	   */
	  public static int binarySearch(short[] a, int fromIndex, int toIndex,
			  short key) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  return binarySearch0(a, fromIndex, toIndex, key);
	  }

	  // Like public version, but without range checks.
	  private static int binarySearch0(short[] a, int fromIndex, int toIndex,
			  short key) {
		  int low = fromIndex;
		  int high = toIndex - 1;

		  while (low <= high) {
			  int mid = (low + high) >>> 1;
		  short midVal = a[mid];

		  if (midVal < key)
			  low = mid + 1;
		  else if (midVal > key)
			  high = mid - 1;
		  else
			  return mid; // key found
		  }
		  return -(low + 1);  // key not found.
	  }

	  /**
	   * Searches the specified array of chars for the specified value using the
	   * binary search algorithm.  The array must be sorted (as
	   * by the {@link #sort(char[])} method) prior to making this call.  If it
	   * is not sorted, the results are undefined.  If the array contains
	   * multiple elements with the specified value, there is no guarantee which
	   * one will be found.
	   *
	   * @param a the array to be searched
	   * @param key the value to be searched for
	   * @return index of the search key, if it is contained in the array;
	   *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
	   *	       <i>insertion point</i> is defined as the point at which the
	   *	       key would be inserted into the array: the index of the first
	   *	       element greater than the key, or <tt>a.length</tt> if all
	   *	       elements in the array are less than the specified key.  Note
	   *	       that this guarantees that the return value will be &gt;= 0 if
	   *	       and only if the key is found.
	   */
	  public static int binarySearch(char[] a, char key) {
		  return binarySearch0(a, 0, a.length, key);
	  }

	  /**
	   * Searches a range of
	   * the specified array of chars for the specified value using the
	   * binary search algorithm.
	   * The range must be sorted (as
	   * by the {@link #sort(char[], int, int)} method)
	   * prior to making this call.  If it
	   * is not sorted, the results are undefined.  If the range contains
	   * multiple elements with the specified value, there is no guarantee which
	   * one will be found.
	   *
	   * @param a the array to be searched
	   * @param fromIndex the index of the first element (inclusive) to be
	   *		searched
	   * @param toIndex the index of the last element (exclusive) to be searched
	   * @param key the value to be searched for
	   * @return index of the search key, if it is contained in the array
	   *	       within the specified range;
	   *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
	   *	       <i>insertion point</i> is defined as the point at which the
	   *	       key would be inserted into the array: the index of the first
	   *	       element in the range greater than the key,
	   *	       or <tt>toIndex</tt> if all
	   *	       elements in the range are less than the specified key.  Note
	   *	       that this guarantees that the return value will be &gt;= 0 if
	   *	       and only if the key is found.
	   * @throws IllegalArgumentException
	   *	       if {@code fromIndex > toIndex}
	   * @throws ArrayIndexOutOfBoundsException
	   *	       if {@code fromIndex < 0 or toIndex > a.length}
	   * @since 1.6
	   */
	  public static int binarySearch(char[] a, int fromIndex, int toIndex,
			  char key) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  return binarySearch0(a, fromIndex, toIndex, key);
	  }

	  // Like public version, but without range checks.
	  private static int binarySearch0(char[] a, int fromIndex, int toIndex,
			  char key) {
		  int low = fromIndex;
		  int high = toIndex - 1;

		  while (low <= high) {
			  int mid = (low + high) >>> 1;
			  char midVal = a[mid];

			  if (midVal < key)
				  low = mid + 1;
			  else if (midVal > key)
				  high = mid - 1;
			  else
				  return mid; // key found
		  }
		  return -(low + 1);  // key not found.
	  }

	  /**
	   * Searches the specified array of bytes for the specified value using the
	   * binary search algorithm.  The array must be sorted (as
	   * by the {@link #sort(byte[])} method) prior to making this call.  If it
	   * is not sorted, the results are undefined.  If the array contains
	   * multiple elements with the specified value, there is no guarantee which
	   * one will be found.
	   *
	   * @param a the array to be searched
	   * @param key the value to be searched for
	   * @return index of the search key, if it is contained in the array;
	   *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
	   *	       <i>insertion point</i> is defined as the point at which the
	   *	       key would be inserted into the array: the index of the first
	   *	       element greater than the key, or <tt>a.length</tt> if all
	   *	       elements in the array are less than the specified key.  Note
	   *	       that this guarantees that the return value will be &gt;= 0 if
	   *	       and only if the key is found.
	   */
	  public static int binarySearch(byte[] a, byte key) {
		  return binarySearch0(a, 0, a.length, key);
	  }

	  /**
	   * Searches a range of
	   * the specified array of bytes for the specified value using the
	   * binary search algorithm.
	   * The range must be sorted (as
	   * by the {@link #sort(byte[], int, int)} method)
	   * prior to making this call.  If it
	   * is not sorted, the results are undefined.  If the range contains
	   * multiple elements with the specified value, there is no guarantee which
	   * one will be found.
	   *
	   * @param a the array to be searched
	   * @param fromIndex the index of the first element (inclusive) to be
	   *		searched
	   * @param toIndex the index of the last element (exclusive) to be searched
	   * @param key the value to be searched for
	   * @return index of the search key, if it is contained in the array
	   *	       within the specified range;
	   *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
	   *	       <i>insertion point</i> is defined as the point at which the
	   *	       key would be inserted into the array: the index of the first
	   *	       element in the range greater than the key,
	   *	       or <tt>toIndex</tt> if all
	   *	       elements in the range are less than the specified key.  Note
	   *	       that this guarantees that the return value will be &gt;= 0 if
	   *	       and only if the key is found.
	   * @throws IllegalArgumentException
	   *	       if {@code fromIndex > toIndex}
	   * @throws ArrayIndexOutOfBoundsException
	   *	       if {@code fromIndex < 0 or toIndex > a.length}
	   * @since 1.6
	   */
	  public static int binarySearch(byte[] a, int fromIndex, int toIndex,
			  byte key) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  return binarySearch0(a, fromIndex, toIndex, key);
	  }

	  // Like public version, but without range checks.
	  private static int binarySearch0(byte[] a, int fromIndex, int toIndex,
			  byte key) {
		  int low = fromIndex;
		  int high = toIndex - 1;

		  while (low <= high) {
			  int mid = (low + high) >>> 1;
		  byte midVal = a[mid];

		  if (midVal < key)
			  low = mid + 1;
		  else if (midVal > key)
			  high = mid - 1;
		  else
			  return mid; // key found
		  }
		  return -(low + 1);  // key not found.
	  }

	  /**
	   * Searches the specified array of doubles for the specified value using
	   * the binary search algorithm.  The array must be sorted
	   * (as by the {@link #sort(double[])} method) prior to making this call.
	   * If it is not sorted, the results are undefined.  If the array contains
	   * multiple elements with the specified value, there is no guarantee which
	   * one will be found.  This method considers all NaN values to be
	   * equivalent and equal.
	   *
	   * @param a the array to be searched
	   * @param key the value to be searched for
	   * @return index of the search key, if it is contained in the array;
	   *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
	   *	       <i>insertion point</i> is defined as the point at which the
	   *	       key would be inserted into the array: the index of the first
	   *	       element greater than the key, or <tt>a.length</tt> if all
	   *	       elements in the array are less than the specified key.  Note
	   *	       that this guarantees that the return value will be &gt;= 0 if
	   *	       and only if the key is found.
	   */
	  public static int binarySearch(double[] a, double key) {
		  return binarySearch0(a, 0, a.length, key);
	  }

	  /**
	   * Searches a range of
	   * the specified array of doubles for the specified value using
	   * the binary search algorithm.
	   * The range must be sorted
	   * (as by the {@link #sort(double[], int, int)} method)
	   * prior to making this call.
	   * If it is not sorted, the results are undefined.  If the range contains
	   * multiple elements with the specified value, there is no guarantee which
	   * one will be found.  This method considers all NaN values to be
	   * equivalent and equal.
	   *
	   * @param a the array to be searched
	   * @param fromIndex the index of the first element (inclusive) to be
	   *		searched
	   * @param toIndex the index of the last element (exclusive) to be searched
	   * @param key the value to be searched for
	   * @return index of the search key, if it is contained in the array
	   *	       within the specified range;
	   *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
	   *	       <i>insertion point</i> is defined as the point at which the
	   *	       key would be inserted into the array: the index of the first
	   *	       element in the range greater than the key,
	   *	       or <tt>toIndex</tt> if all
	   *	       elements in the range are less than the specified key.  Note
	   *	       that this guarantees that the return value will be &gt;= 0 if
	   *	       and only if the key is found.
	   * @throws IllegalArgumentException
	   *	       if {@code fromIndex > toIndex}
	   * @throws ArrayIndexOutOfBoundsException
	   *	       if {@code fromIndex < 0 or toIndex > a.length}
	   * @since 1.6
	   */
	  public static int binarySearch(double[] a, int fromIndex, int toIndex,
			  double key) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  return binarySearch0(a, fromIndex, toIndex, key);
	  }

	  // Like public version, but without range checks.
	  private static int binarySearch0(double[] a, int fromIndex, int toIndex,
			  double key) {
		  int low = fromIndex;
		  int high = toIndex - 1;

		  while (low <= high) {
			  int mid = (low + high) >>> 1;
		  double midVal = a[mid];

		  int cmp;
		  if (midVal < key) {
			  cmp = -1;   // Neither val is NaN, thisVal is smaller
		  } else if (midVal > key) {
			  cmp = 1;    // Neither val is NaN, thisVal is larger
		  } else {
			  long midBits = Double.doubleToLongBits(midVal);
			  long keyBits = Double.doubleToLongBits(key);
			  cmp = (midBits == keyBits ?  0 : // Values are equal
				  (midBits < keyBits ? -1 : // (-0.0, 0.0) or (!NaN, NaN)
					  1));                     // (0.0, -0.0) or (NaN, !NaN)
		  }

		  if (cmp < 0)
			  low = mid + 1;
		  else if (cmp > 0)
			  high = mid - 1;
		  else
			  return mid; // key found
		  }
		  return -(low + 1);  // key not found.
	  }

	  /**
	   * Searches the specified array of floats for the specified value using
	   * the binary search algorithm.  The array must be sorted
	   * (as by the {@link #sort(float[])} method) prior to making this call.  If
	   * it is not sorted, the results are undefined.  If the array contains
	   * multiple elements with the specified value, there is no guarantee which
	   * one will be found.  This method considers all NaN values to be
	   * equivalent and equal.
	   *
	   * @param a the array to be searched
	   * @param key the value to be searched for
	   * @return index of the search key, if it is contained in the array;
	   *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
	   *	       <i>insertion point</i> is defined as the point at which the
	   *	       key would be inserted into the array: the index of the first
	   *	       element greater than the key, or <tt>a.length</tt> if all
	   *	       elements in the array are less than the specified key.  Note
	   *	       that this guarantees that the return value will be &gt;= 0 if
	   *	       and only if the key is found.
	   */
	  public static int binarySearch(float[] a, float key) {
		  return binarySearch0(a, 0, a.length, key);
	  }

	  /**
	   * Searches a range of
	   * the specified array of floats for the specified value using
	   * the binary search algorithm.
	   * The range must be sorted
	   * (as by the {@link #sort(float[], int, int)} method)
	   * prior to making this call.  If
	   * it is not sorted, the results are undefined.  If the range contains
	   * multiple elements with the specified value, there is no guarantee which
	   * one will be found.  This method considers all NaN values to be
	   * equivalent and equal.
	   *
	   * @param a the array to be searched
	   * @param fromIndex the index of the first element (inclusive) to be
	   *		searched
	   * @param toIndex the index of the last element (exclusive) to be searched
	   * @param key the value to be searched for
	   * @return index of the search key, if it is contained in the array
	   *	       within the specified range;
	   *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
	   *	       <i>insertion point</i> is defined as the point at which the
	   *	       key would be inserted into the array: the index of the first
	   *	       element in the range greater than the key,
	   *	       or <tt>toIndex</tt> if all
	   *	       elements in the range are less than the specified key.  Note
	   *	       that this guarantees that the return value will be &gt;= 0 if
	   *	       and only if the key is found.
	   * @throws IllegalArgumentException
	   *	       if {@code fromIndex > toIndex}
	   * @throws ArrayIndexOutOfBoundsException
	   *	       if {@code fromIndex < 0 or toIndex > a.length}
	   * @since 1.6
	   */
	  public static int binarySearch(float[] a, int fromIndex, int toIndex,
			  float key) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  return binarySearch0(a, fromIndex, toIndex, key);
	  }

	  // Like public version, but without range checks.
	  private static int binarySearch0(float[] a, int fromIndex, int toIndex,
			  float key) {
		  int low = fromIndex;
		  int high = toIndex - 1;

		  while (low <= high) {
			  int mid = (low + high) >>> 1;
				  float midVal = a[mid];

				  int cmp;
				  if (midVal < key) {
					  cmp = -1;   // Neither val is NaN, thisVal is smaller
				  } else if (midVal > key) {
					  cmp = 1;    // Neither val is NaN, thisVal is larger
				  } else {
					  int midBits = Float.floatToIntBits(midVal);
					  int keyBits = Float.floatToIntBits(key);
					  cmp = (midBits == keyBits ?  0 : // Values are equal
						  (midBits < keyBits ? -1 : // (-0.0, 0.0) or (!NaN, NaN)
							  1));                     // (0.0, -0.0) or (NaN, !NaN)
				  }

				  if (cmp < 0)
					  low = mid + 1;
				  else if (cmp > 0)
					  high = mid - 1;
				  else
					  return mid; // key found
		  }
		  return -(low + 1);  // key not found.
	  }


	  /**
	   * Searches the specified array for the specified object using the binary
	   * search algorithm.  The array must be sorted into ascending order
	   * according to the
	   * {@linkplain Comparable natural ordering}
	   * of its elements (as by the
	   * {@link #sort(Object[])} method) prior to making this call.
	   * If it is not sorted, the results are undefined.
	   * (If the array contains elements that are not mutually comparable (for
	   * example, strings and integers), it <i>cannot</i> be sorted according
	   * to the natural ordering of its elements, hence results are undefined.)
	   * If the array contains multiple
	   * elements equal to the specified object, there is no guarantee which
	   * one will be found.
	   *
	   * @param a the array to be searched
	   * @param key the value to be searched for
	   * @return index of the search key, if it is contained in the array;
	   *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
	   *	       <i>insertion point</i> is defined as the point at which the
	   *	       key would be inserted into the array: the index of the first
	   *	       element greater than the key, or <tt>a.length</tt> if all
	   *	       elements in the array are less than the specified key.  Note
	   *	       that this guarantees that the return value will be &gt;= 0 if
	   *	       and only if the key is found.
	   * @throws ClassCastException if the search key is not comparable to the
	   *         elements of the array.
	   */
	  public static int binarySearch(Object[] a, Object key) {
		  return binarySearch0(a, 0, a.length, key);
	  }

	  /**
	   * Searches a range of
	   * the specified array for the specified object using the binary
	   * search algorithm.
	   * The range must be sorted into ascending order
	   * according to the
	   * {@linkplain Comparable natural ordering}
	   * of its elements (as by the
	   * {@link #sort(Object[], int, int)} method) prior to making this
	   * call.  If it is not sorted, the results are undefined.
	   * (If the range contains elements that are not mutually comparable (for
	   * example, strings and integers), it <i>cannot</i> be sorted according
	   * to the natural ordering of its elements, hence results are undefined.)
	   * If the range contains multiple
	   * elements equal to the specified object, there is no guarantee which
	   * one will be found.
	   *
	   * @param a the array to be searched
	   * @param fromIndex the index of the first element (inclusive) to be
	   *		searched
	   * @param toIndex the index of the last element (exclusive) to be searched
	   * @param key the value to be searched for
	   * @return index of the search key, if it is contained in the array
	   *	       within the specified range;
	   *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
	   *	       <i>insertion point</i> is defined as the point at which the
	   *	       key would be inserted into the array: the index of the first
	   *	       element in the range greater than the key,
	   *	       or <tt>toIndex</tt> if all
	   *	       elements in the range are less than the specified key.  Note
	   *	       that this guarantees that the return value will be &gt;= 0 if
	   *	       and only if the key is found.
	   * @throws ClassCastException if the search key is not comparable to the
	   *         elements of the array within the specified range.
	   * @throws IllegalArgumentException
	   *	       if {@code fromIndex > toIndex}
	   * @throws ArrayIndexOutOfBoundsException
	   *	       if {@code fromIndex < 0 or toIndex > a.length}
	   * @since 1.6
	   */
	  public static int binarySearch(Object[] a, int fromIndex, int toIndex,
			  Object key) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  return binarySearch0(a, fromIndex, toIndex, key);
	  }

	  // Like public version, but without range checks.
	  private static int binarySearch0(Object[] a, int fromIndex, int toIndex,
			  Object key) {
		  int low = fromIndex;
		  int high = toIndex - 1;

		  while (low <= high) {
			  int mid = (low + high) >>> 1;
						  Comparable midVal = (Comparable)a[mid];
						  int cmp = midVal.compareTo(key);

						  if (cmp < 0)
							  low = mid + 1;
						  else if (cmp > 0)
							  high = mid - 1;
						  else
							  return mid; // key found
		  }
		  return -(low + 1);  // key not found.
	  }

	  /**
	   * Searches the specified array for the specified object using the binary
	   * search algorithm.  The array must be sorted into ascending order
	   * according to the specified comparator (as by the
	   * {@link #sort(Object[], Comparator) sort(T[], Comparator)}
	   * method) prior to making this call.  If it is
	   * not sorted, the results are undefined.
	   * If the array contains multiple
	   * elements equal to the specified object, there is no guarantee which one
	   * will be found.
	   *
	   * @param a the array to be searched
	   * @param key the value to be searched for
	   * @param c the comparator by which the array is ordered.  A
	   *        <tt>null</tt> value indicates that the elements'
	   *	      {@linkplain Comparable natural ordering} should be used.
	   * @return index of the search key, if it is contained in the array;
	   *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
	   *	       <i>insertion point</i> is defined as the point at which the
	   *	       key would be inserted into the array: the index of the first
	   *	       element greater than the key, or <tt>a.length</tt> if all
	   *	       elements in the array are less than the specified key.  Note
	   *	       that this guarantees that the return value will be &gt;= 0 if
	   *	       and only if the key is found.
	   * @throws ClassCastException if the array contains elements that are not
	   *	       <i>mutually comparable</i> using the specified comparator,
	   *	       or the search key is not comparable to the
	   *	       elements of the array using this comparator.
	   */
	  public static <T> int binarySearch(T[] a, T key, Comparator<? super T> c) {
		  return binarySearch0(a, 0, a.length, key, c);
	  }

	  /**
	   * Searches a range of
	   * the specified array for the specified object using the binary
	   * search algorithm.
	   * The range must be sorted into ascending order
	   * according to the specified comparator (as by the
	   * {@link #sort(Object[], int, int, Comparator)
	   * sort(T[], int, int, Comparator)}
	   * method) prior to making this call.
	   * If it is not sorted, the results are undefined.
	   * If the range contains multiple elements equal to the specified object,
	   * there is no guarantee which one will be found.
	   *
	   * @param a the array to be searched
	   * @param fromIndex the index of the first element (inclusive) to be
	   *		searched
	   * @param toIndex the index of the last element (exclusive) to be searched
	   * @param key the value to be searched for
	   * @param c the comparator by which the array is ordered.  A
	   *        <tt>null</tt> value indicates that the elements'
	   *        {@linkplain Comparable natural ordering} should be used.
	   * @return index of the search key, if it is contained in the array
	   *	       within the specified range;
	   *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
	   *	       <i>insertion point</i> is defined as the point at which the
	   *	       key would be inserted into the array: the index of the first
	   *	       element in the range greater than the key,
	   *	       or <tt>toIndex</tt> if all
	   *	       elements in the range are less than the specified key.  Note
	   *	       that this guarantees that the return value will be &gt;= 0 if
	   *	       and only if the key is found.
	   * @throws ClassCastException if the range contains elements that are not
	   *	       <i>mutually comparable</i> using the specified comparator,
	   *	       or the search key is not comparable to the
	   *	       elements in the range using this comparator.
	   * @throws IllegalArgumentException
	   *	       if {@code fromIndex > toIndex}
	   * @throws ArrayIndexOutOfBoundsException
	   *	       if {@code fromIndex < 0 or toIndex > a.length}
	   * @since 1.6
	   */
	  public static <T> int binarySearch(T[] a, int fromIndex, int toIndex,
			  T key, Comparator<? super T> c) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  return binarySearch0(a, fromIndex, toIndex, key, c);
	  }

	  // Like public version, but without range checks.
	  private static <T> int binarySearch0(T[] a, int fromIndex, int toIndex,
			  T key, Comparator<? super T> c) {
		  if (c == null) {
			  return binarySearch0(a, fromIndex, toIndex, key);
		  }
		  int low = fromIndex;
		  int high = toIndex - 1;

		  while (low <= high) {
			  int mid = (low + high) >>> 1;
		  T midVal = a[mid];
		  int cmp = c.compare(midVal, key);

		  if (cmp < 0)
			  low = mid + 1;
		  else if (cmp > 0)
			  high = mid - 1;
		  else
			  return mid; // key found
		  }
		  return -(low + 1);  // key not found.
	  }


	  // Equality Testing

	  /**
	   * Returns <tt>true</tt> if the two specified arrays of longs are
	   * <i>equal</i> to one another.  Two arrays are considered equal if both
	   * arrays contain the same number of elements, and all corresponding pairs
	   * of elements in the two arrays are equal.  In other words, two arrays
	   * are equal if they contain the same elements in the same order.  Also,
	   * two array references are considered equal if both are <tt>null</tt>.<p>
	   *
	   * @param a one array to be tested for equality
	   * @param a2 the other array to be tested for equality
	   * @return <tt>true</tt> if the two arrays are equal
	   */
	  public static boolean equals(long[] a, long[] a2) {
		  if (a==a2)
			  return true;
		  if (a==null || a2==null)
			  return false;

		  int length = a.length;
		  if (a2.length != length)
			  return false;

		  for (int i=0; i<length; i++)
			  if (a[i] != a2[i])
				  return false;

		  return true;
	  }

	  /**
	   * Returns <tt>true</tt> if the two specified arrays of ints are
	   * <i>equal</i> to one another.  Two arrays are considered equal if both
	   * arrays contain the same number of elements, and all corresponding pairs
	   * of elements in the two arrays are equal.  In other words, two arrays
	   * are equal if they contain the same elements in the same order.  Also,
	   * two array references are considered equal if both are <tt>null</tt>.<p>
	   *
	   * @param a one array to be tested for equality
	   * @param a2 the other array to be tested for equality
	   * @return <tt>true</tt> if the two arrays are equal
	   */
	  public static boolean equals(int[] a, int[] a2) {
		  if (a==a2)
			  return true;
		  if (a==null || a2==null)
			  return false;

		  int length = a.length;
		  if (a2.length != length)
			  return false;

		  for (int i=0; i<length; i++)
			  if (a[i] != a2[i])
				  return false;

		  return true;
	  }

	  /**
	   * Returns <tt>true</tt> if the two specified arrays of shorts are
	   * <i>equal</i> to one another.  Two arrays are considered equal if both
	   * arrays contain the same number of elements, and all corresponding pairs
	   * of elements in the two arrays are equal.  In other words, two arrays
	   * are equal if they contain the same elements in the same order.  Also,
	   * two array references are considered equal if both are <tt>null</tt>.<p>
	   *
	   * @param a one array to be tested for equality
	   * @param a2 the other array to be tested for equality
	   * @return <tt>true</tt> if the two arrays are equal
	   */
	  public static boolean equals(short[] a, short a2[]) {
		  if (a==a2)
			  return true;
		  if (a==null || a2==null)
			  return false;

		  int length = a.length;
		  if (a2.length != length)
			  return false;

		  for (int i=0; i<length; i++)
			  if (a[i] != a2[i])
				  return false;

		  return true;
	  }

	  /**
	   * Returns <tt>true</tt> if the two specified arrays of chars are
	   * <i>equal</i> to one another.  Two arrays are considered equal if both
	   * arrays contain the same number of elements, and all corresponding pairs
	   * of elements in the two arrays are equal.  In other words, two arrays
	   * are equal if they contain the same elements in the same order.  Also,
	   * two array references are considered equal if both are <tt>null</tt>.<p>
	   *
	   * @param a one array to be tested for equality
	   * @param a2 the other array to be tested for equality
	   * @return <tt>true</tt> if the two arrays are equal
	   */
	  public static boolean equals(char[] a, char[] a2) {
		  if (a==a2)
			  return true;
		  if (a==null || a2==null)
			  return false;

		  int length = a.length;
		  if (a2.length != length)
			  return false;

		  for (int i=0; i<length; i++)
			  if (a[i] != a2[i])
				  return false;

		  return true;
	  }

	  /**
	   * Returns <tt>true</tt> if the two specified arrays of bytes are
	   * <i>equal</i> to one another.  Two arrays are considered equal if both
	   * arrays contain the same number of elements, and all corresponding pairs
	   * of elements in the two arrays are equal.  In other words, two arrays
	   * are equal if they contain the same elements in the same order.  Also,
	   * two array references are considered equal if both are <tt>null</tt>.<p>
	   *
	   * @param a one array to be tested for equality
	   * @param a2 the other array to be tested for equality
	   * @return <tt>true</tt> if the two arrays are equal
	   */
	  public static boolean equals(byte[] a, byte[] a2) {
		  if (a==a2)
			  return true;
		  if (a==null || a2==null)
			  return false;

		  int length = a.length;
		  if (a2.length != length)
			  return false;

		  for (int i=0; i<length; i++)
			  if (a[i] != a2[i])
				  return false;

		  return true;
	  }

	  /**
	   * Returns <tt>true</tt> if the two specified arrays of booleans are
	   * <i>equal</i> to one another.  Two arrays are considered equal if both
	   * arrays contain the same number of elements, and all corresponding pairs
	   * of elements in the two arrays are equal.  In other words, two arrays
	   * are equal if they contain the same elements in the same order.  Also,
	   * two array references are considered equal if both are <tt>null</tt>.<p>
	   *
	   * @param a one array to be tested for equality
	   * @param a2 the other array to be tested for equality
	   * @return <tt>true</tt> if the two arrays are equal
	   */
	  public static boolean equals(boolean[] a, boolean[] a2) {
		  if (a==a2)
			  return true;
		  if (a==null || a2==null)
			  return false;

		  int length = a.length;
		  if (a2.length != length)
			  return false;

		  for (int i=0; i<length; i++)
			  if (a[i] != a2[i])
				  return false;

		  return true;
	  }

	  /**
	   * Returns <tt>true</tt> if the two specified arrays of doubles are
	   * <i>equal</i> to one another.  Two arrays are considered equal if both
	   * arrays contain the same number of elements, and all corresponding pairs
	   * of elements in the two arrays are equal.  In other words, two arrays
	   * are equal if they contain the same elements in the same order.  Also,
	   * two array references are considered equal if both are <tt>null</tt>.<p>
	   *
	   * Two doubles <tt>d1</tt> and <tt>d2</tt> are considered equal if:
	   * <pre>    <tt>new Double(d1).equals(new Double(d2))</tt></pre>
	   * (Unlike the <tt>==</tt> operator, this method considers
	   * <tt>NaN</tt> equals to itself, and 0.0d unequal to -0.0d.)
	   *
	   * @param a one array to be tested for equality
	   * @param a2 the other array to be tested for equality
	   * @return <tt>true</tt> if the two arrays are equal
	   * @see Double#equals(Object)
	   */
	  public static boolean equals(double[] a, double[] a2) {
		  if (a==a2)
			  return true;
		  if (a==null || a2==null)
			  return false;

		  int length = a.length;
		  if (a2.length != length)
			  return false;

		  for (int i=0; i<length; i++)
			  if (Double.doubleToLongBits(a[i])!=Double.doubleToLongBits(a2[i]))
				  return false;

		  return true;
	  }

	  /**
	   * Returns <tt>true</tt> if the two specified arrays of floats are
	   * <i>equal</i> to one another.  Two arrays are considered equal if both
	   * arrays contain the same number of elements, and all corresponding pairs
	   * of elements in the two arrays are equal.  In other words, two arrays
	   * are equal if they contain the same elements in the same order.  Also,
	   * two array references are considered equal if both are <tt>null</tt>.<p>
	   *
	   * Two floats <tt>f1</tt> and <tt>f2</tt> are considered equal if:
	   * <pre>    <tt>new Float(f1).equals(new Float(f2))</tt></pre>
	   * (Unlike the <tt>==</tt> operator, this method considers
	   * <tt>NaN</tt> equals to itself, and 0.0f unequal to -0.0f.)
	   *
	   * @param a one array to be tested for equality
	   * @param a2 the other array to be tested for equality
	   * @return <tt>true</tt> if the two arrays are equal
	   * @see Float#equals(Object)
	   */
	  public static boolean equals(float[] a, float[] a2) {
		  if (a==a2)
			  return true;
		  if (a==null || a2==null)
			  return false;

		  int length = a.length;
		  if (a2.length != length)
			  return false;

		  for (int i=0; i<length; i++)
			  if (Float.floatToIntBits(a[i])!=Float.floatToIntBits(a2[i]))
				  return false;

		  return true;
	  }


	  /**
	   * Returns <tt>true</tt> if the two specified arrays of Objects are
	   * <i>equal</i> to one another.  The two arrays are considered equal if
	   * both arrays contain the same number of elements, and all corresponding
	   * pairs of elements in the two arrays are equal.  Two objects <tt>e1</tt>
	   * and <tt>e2</tt> are considered <i>equal</i> if <tt>(e1==null ? e2==null
	   * : e1.equals(e2))</tt>.  In other words, the two arrays are equal if
	   * they contain the same elements in the same order.  Also, two array
	   * references are considered equal if both are <tt>null</tt>.<p>
	   *
	   * @param a one array to be tested for equality
	   * @param a2 the other array to be tested for equality
	   * @return <tt>true</tt> if the two arrays are equal
	   */
	  public static boolean equals(Object[] a, Object[] a2) {
		  if (a==a2)
			  return true;
		  if (a==null || a2==null)
			  return false;

		  int length = a.length;
		  if (a2.length != length)
			  return false;

		  for (int i=0; i<length; i++) {
			  Object o1 = a[i];
			  Object o2 = a2[i];
			  if (!(o1==null ? o2==null : o1.equals(o2)))
				  return false;
		  }

		  return true;
	  }


	  // Filling

	  /**
	   * Assigns the specified long value to each element of the specified array
	   * of longs.
	   *
	   * @param a the array to be filled
	   * @param val the value to be stored in all elements of the array
	   */
	  public static void fill(long[] a, long val) {
		  fill(a, 0, a.length, val);
	  }

	  /**
	   * Assigns the specified long value to each element of the specified
	   * range of the specified array of longs.  The range to be filled
	   * extends from index <tt>fromIndex</tt>, inclusive, to index
	   * <tt>toIndex</tt>, exclusive.  (If <tt>fromIndex==toIndex</tt>, the
	   * range to be filled is empty.)
	   *
	   * @param a the array to be filled
	   * @param fromIndex the index of the first element (inclusive) to be
	   *        filled with the specified value
	   * @param toIndex the index of the last element (exclusive) to be
	   *        filled with the specified value
	   * @param val the value to be stored in all elements of the array
	   * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	   * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	   *	       <tt>toIndex &gt; a.length</tt>
	   */
	  public static void fill(long[] a, int fromIndex, int toIndex, long val) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  for (int i=fromIndex; i<toIndex; i++)
			  a[i] = val;
	  }

	  /**
	   * Assigns the specified int value to each element of the specified array
	   * of ints.
	   *
	   * @param a the array to be filled
	   * @param val the value to be stored in all elements of the array
	   */
	  public static void fill(int[] a, int val) {
		  fill(a, 0, a.length, val);
	  }

	  /**
	   * Assigns the specified int value to each element of the specified
	   * range of the specified array of ints.  The range to be filled
	   * extends from index <tt>fromIndex</tt>, inclusive, to index
	   * <tt>toIndex</tt>, exclusive.  (If <tt>fromIndex==toIndex</tt>, the
	   * range to be filled is empty.)
	   *
	   * @param a the array to be filled
	   * @param fromIndex the index of the first element (inclusive) to be
	   *        filled with the specified value
	   * @param toIndex the index of the last element (exclusive) to be
	   *        filled with the specified value
	   * @param val the value to be stored in all elements of the array
	   * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	   * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	   *	       <tt>toIndex &gt; a.length</tt>
	   */
	  public static void fill(int[] a, int fromIndex, int toIndex, int val) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  for (int i=fromIndex; i<toIndex; i++)
			  a[i] = val;
	  }

	  /**
	   * Assigns the specified short value to each element of the specified array
	   * of shorts.
	   *
	   * @param a the array to be filled
	   * @param val the value to be stored in all elements of the array
	   */
	  public static void fill(short[] a, short val) {
		  fill(a, 0, a.length, val);
	  }

	  /**
	   * Assigns the specified short value to each element of the specified
	   * range of the specified array of shorts.  The range to be filled
	   * extends from index <tt>fromIndex</tt>, inclusive, to index
	   * <tt>toIndex</tt>, exclusive.  (If <tt>fromIndex==toIndex</tt>, the
	   * range to be filled is empty.)
	   *
	   * @param a the array to be filled
	   * @param fromIndex the index of the first element (inclusive) to be
	   *        filled with the specified value
	   * @param toIndex the index of the last element (exclusive) to be
	   *        filled with the specified value
	   * @param val the value to be stored in all elements of the array
	   * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	   * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	   *	       <tt>toIndex &gt; a.length</tt>
	   */
	  public static void fill(short[] a, int fromIndex, int toIndex, short val) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  for (int i=fromIndex; i<toIndex; i++)
			  a[i] = val;
	  }

	  /**
	   * Assigns the specified char value to each element of the specified array
	   * of chars.
	   *
	   * @param a the array to be filled
	   * @param val the value to be stored in all elements of the array
	   */
	  public static void fill(char[] a, char val) {
		  fill(a, 0, a.length, val);
	  }

	  /**
	   * Assigns the specified char value to each element of the specified
	   * range of the specified array of chars.  The range to be filled
	   * extends from index <tt>fromIndex</tt>, inclusive, to index
	   * <tt>toIndex</tt>, exclusive.  (If <tt>fromIndex==toIndex</tt>, the
	   * range to be filled is empty.)
	   *
	   * @param a the array to be filled
	   * @param fromIndex the index of the first element (inclusive) to be
	   *        filled with the specified value
	   * @param toIndex the index of the last element (exclusive) to be
	   *        filled with the specified value
	   * @param val the value to be stored in all elements of the array
	   * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	   * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	   *	       <tt>toIndex &gt; a.length</tt>
	   */
	  public static void fill(char[] a, int fromIndex, int toIndex, char val) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  for (int i=fromIndex; i<toIndex; i++)
			  a[i] = val;
	  }

	  /**
	   * Assigns the specified byte value to each element of the specified array
	   * of bytes.
	   *
	   * @param a the array to be filled
	   * @param val the value to be stored in all elements of the array
	   */
	  public static void fill(byte[] a, byte val) {
		  fill(a, 0, a.length, val);
	  }

	  /**
	   * Assigns the specified byte value to each element of the specified
	   * range of the specified array of bytes.  The range to be filled
	   * extends from index <tt>fromIndex</tt>, inclusive, to index
	   * <tt>toIndex</tt>, exclusive.  (If <tt>fromIndex==toIndex</tt>, the
	   * range to be filled is empty.)
	   *
	   * @param a the array to be filled
	   * @param fromIndex the index of the first element (inclusive) to be
	   *        filled with the specified value
	   * @param toIndex the index of the last element (exclusive) to be
	   *        filled with the specified value
	   * @param val the value to be stored in all elements of the array
	   * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	   * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	   *	       <tt>toIndex &gt; a.length</tt>
	   */
	  public static void fill(byte[] a, int fromIndex, int toIndex, byte val) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  for (int i=fromIndex; i<toIndex; i++)
			  a[i] = val;
	  }

	  /**
	   * Assigns the specified boolean value to each element of the specified
	   * array of booleans.
	   *
	   * @param a the array to be filled
	   * @param val the value to be stored in all elements of the array
	   */
	  public static void fill(boolean[] a, boolean val) {
		  fill(a, 0, a.length, val);
	  }

	  /**
	   * Assigns the specified boolean value to each element of the specified
	   * range of the specified array of booleans.  The range to be filled
	   * extends from index <tt>fromIndex</tt>, inclusive, to index
	   * <tt>toIndex</tt>, exclusive.  (If <tt>fromIndex==toIndex</tt>, the
	   * range to be filled is empty.)
	   *
	   * @param a the array to be filled
	   * @param fromIndex the index of the first element (inclusive) to be
	   *        filled with the specified value
	   * @param toIndex the index of the last element (exclusive) to be
	   *        filled with the specified value
	   * @param val the value to be stored in all elements of the array
	   * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	   * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	   *	       <tt>toIndex &gt; a.length</tt>
	   */
	  public static void fill(boolean[] a, int fromIndex, int toIndex,
			  boolean val) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  for (int i=fromIndex; i<toIndex; i++)
			  a[i] = val;
	  }

	  /**
	   * Assigns the specified double value to each element of the specified
	   * array of doubles.
	   *
	   * @param a the array to be filled
	   * @param val the value to be stored in all elements of the array
	   */
	  public static void fill(double[] a, double val) {
		  fill(a, 0, a.length, val);
	  }

	  /**
	   * Assigns the specified double value to each element of the specified
	   * range of the specified array of doubles.  The range to be filled
	   * extends from index <tt>fromIndex</tt>, inclusive, to index
	   * <tt>toIndex</tt>, exclusive.  (If <tt>fromIndex==toIndex</tt>, the
	   * range to be filled is empty.)
	   *
	   * @param a the array to be filled
	   * @param fromIndex the index of the first element (inclusive) to be
	   *        filled with the specified value
	   * @param toIndex the index of the last element (exclusive) to be
	   *        filled with the specified value
	   * @param val the value to be stored in all elements of the array
	   * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	   * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	   *	       <tt>toIndex &gt; a.length</tt>
	   */
	  public static void fill(double[] a, int fromIndex, int toIndex,double val){
		  rangeCheck(a.length, fromIndex, toIndex);
		  for (int i=fromIndex; i<toIndex; i++)
			  a[i] = val;
	  }

	  /**
	   * Assigns the specified float value to each element of the specified array
	   * of floats.
	   *
	   * @param a the array to be filled
	   * @param val the value to be stored in all elements of the array
	   */
	  public static void fill(float[] a, float val) {
		  fill(a, 0, a.length, val);
	  }

	  /**
	   * Assigns the specified float value to each element of the specified
	   * range of the specified array of floats.  The range to be filled
	   * extends from index <tt>fromIndex</tt>, inclusive, to index
	   * <tt>toIndex</tt>, exclusive.  (If <tt>fromIndex==toIndex</tt>, the
	   * range to be filled is empty.)
	   *
	   * @param a the array to be filled
	   * @param fromIndex the index of the first element (inclusive) to be
	   *        filled with the specified value
	   * @param toIndex the index of the last element (exclusive) to be
	   *        filled with the specified value
	   * @param val the value to be stored in all elements of the array
	   * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	   * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	   *	       <tt>toIndex &gt; a.length</tt>
	   */
	  public static void fill(float[] a, int fromIndex, int toIndex, float val) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  for (int i=fromIndex; i<toIndex; i++)
			  a[i] = val;
	  }

	  /**
	   * Assigns the specified Object reference to each element of the specified
	   * array of Objects.
	   *
	   * @param a the array to be filled
	   * @param val the value to be stored in all elements of the array
	   * @throws ArrayStoreException if the specified value is not of a
	   *         runtime type that can be stored in the specified array
	   */
	  public static void fill(Object[] a, Object val) {
		  fill(a, 0, a.length, val);
	  }

	  /**
	   * Assigns the specified Object reference to each element of the specified
	   * range of the specified array of Objects.  The range to be filled
	   * extends from index <tt>fromIndex</tt>, inclusive, to index
	   * <tt>toIndex</tt>, exclusive.  (If <tt>fromIndex==toIndex</tt>, the
	   * range to be filled is empty.)
	   *
	   * @param a the array to be filled
	   * @param fromIndex the index of the first element (inclusive) to be
	   *        filled with the specified value
	   * @param toIndex the index of the last element (exclusive) to be
	   *        filled with the specified value
	   * @param val the value to be stored in all elements of the array
	   * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	   * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	   *	       <tt>toIndex &gt; a.length</tt>
	   * @throws ArrayStoreException if the specified value is not of a
	   *         runtime type that can be stored in the specified array
	   */
	  public static void fill(Object[] a, int fromIndex, int toIndex, Object val) {
		  rangeCheck(a.length, fromIndex, toIndex);
		  for (int i=fromIndex; i<toIndex; i++)
			  a[i] = val;
	  }


	  // Cloning
	  /**
	   * Copies the specified array, truncating or padding with nulls (if necessary)
	   * so the copy has the specified length.  For all indices that are
	   * valid in both the original array and the copy, the two arrays will
	   * contain identical values.  For any indices that are valid in the
	   * copy but not the original, the copy will contain <tt>null</tt>.
	   * Such indices will exist if and only if the specified length
	   * is greater than that of the original array.
	   * The resulting array is of exactly the same class as the original array.
	   *
	   * @param original the array to be copied
	   * @param newLength the length of the copy to be returned
	   * @return a copy of the original array, truncated or padded with nulls
	   *     to obtain the specified length
	   * @throws NegativeArraySizeException if <tt>newLength</tt> is negative
	   * @throws NullPointerException if <tt>original</tt> is null
	   * @since 1.6
	   */
	  public static <T> T[] copyOf(T[] original, int newLength) {
		  return (T[]) copyOf(original, newLength, original.getClass());
	  }

	  /**
	   * Copies the specified array, truncating or padding with nulls (if necessary)
	   * so the copy has the specified length.  For all indices that are
	   * valid in both the original array and the copy, the two arrays will
	   * contain identical values.  For any indices that are valid in the
	   * copy but not the original, the copy will contain <tt>null</tt>.
	   * Such indices will exist if and only if the specified length
	   * is greater than that of the original array.
	   * The resulting array is of the class <tt>newType</tt>.
	   *
	   * @param original the array to be copied
	   * @param newLength the length of the copy to be returned
	   * @param newType the class of the copy to be returned
	   * @return a copy of the original array, truncated or padded with nulls
	   *     to obtain the specified length
	   * @throws NegativeArraySizeException if <tt>newLength</tt> is negative
	   * @throws NullPointerException if <tt>original</tt> is null
	   * @throws ArrayStoreException if an element copied from
	   *     <tt>original</tt> is not of a runtime type that can be stored in
	   *     an array of class <tt>newType</tt>
	   * @since 1.6
	   */
	  public static <T,U> T[] copyOf(U[] original, int newLength, Class<? extends T[]> newType) {
		  T[] copy = ((Object)newType == (Object)Object[].class)
		  ? (T[]) new Object[newLength]
		                     : (T[]) Array.newInstance(newType.getComponentType(), newLength);
		  System.arraycopy(original, 0, copy, 0,
				  Math.min(original.length, newLength));
		  return copy;
	  }

	  /**
	   * Copies the specified array, truncating or padding with zeros (if necessary)
	   * so the copy has the specified length.  For all indices that are
	   * valid in both the original array and the copy, the two arrays will
	   * contain identical values.  For any indices that are valid in the
	   * copy but not the original, the copy will contain <tt>(byte)0</tt>.
	   * Such indices will exist if and only if the specified length
	   * is greater than that of the original array.
	   *
	   * @param original the array to be copied
	   * @param newLength the length of the copy to be returned
	   * @return a copy of the original array, truncated or padded with zeros
	   *     to obtain the specified length
	   * @throws NegativeArraySizeException if <tt>newLength</tt> is negative
	   * @throws NullPointerException if <tt>original</tt> is null
	   * @since 1.6
	   */
	  public static byte[] copyOf(byte[] original, int newLength) {
		  byte[] copy = new byte[newLength];
		  System.arraycopy(original, 0, copy, 0,
				  Math.min(original.length, newLength));
		  return copy;
	  }

	  /**
	   * Copies the specified array, truncating or padding with zeros (if necessary)
	   * so the copy has the specified length.  For all indices that are
	   * valid in both the original array and the copy, the two arrays will
	   * contain identical values.  For any indices that are valid in the
	   * copy but not the original, the copy will contain <tt>(short)0</tt>.
	   * Such indices will exist if and only if the specified length
	   * is greater than that of the original array.
	   *
	   * @param original the array to be copied
	   * @param newLength the length of the copy to be returned
	   * @return a copy of the original array, truncated or padded with zeros
	   *     to obtain the specified length
	   * @throws NegativeArraySizeException if <tt>newLength</tt> is negative
	   * @throws NullPointerException if <tt>original</tt> is null
	   * @since 1.6
	   */
	  public static short[] copyOf(short[] original, int newLength) {
		  short[] copy = new short[newLength];
		  System.arraycopy(original, 0, copy, 0,
				  Math.min(original.length, newLength));
		  return copy;
	  }

	  /**
	   * Copies the specified array, truncating or padding with zeros (if necessary)
	   * so the copy has the specified length.  For all indices that are
	   * valid in both the original array and the copy, the two arrays will
	   * contain identical values.  For any indices that are valid in the
	   * copy but not the original, the copy will contain <tt>0</tt>.
	   * Such indices will exist if and only if the specified length
	   * is greater than that of the original array.
	   *
	   * @param original the array to be copied
	   * @param newLength the length of the copy to be returned
	   * @return a copy of the original array, truncated or padded with zeros
	   *     to obtain the specified length
	   * @throws NegativeArraySizeException if <tt>newLength</tt> is negative
	   * @throws NullPointerException if <tt>original</tt> is null
	   * @since 1.6
	   */
	  public static int[] copyOf(int[] original, int newLength) {
		  int[] copy = new int[newLength];
		  System.arraycopy(original, 0, copy, 0,
				  Math.min(original.length, newLength));
		  return copy;
	  }

	  /**
	   * Copies the specified array, truncating or padding with zeros (if necessary)
	   * so the copy has the specified length.  For all indices that are
	   * valid in both the original array and the copy, the two arrays will
	   * contain identical values.  For any indices that are valid in the
	   * copy but not the original, the copy will contain <tt>0L</tt>.
	   * Such indices will exist if and only if the specified length
	   * is greater than that of the original array.
	   *
	   * @param original the array to be copied
	   * @param newLength the length of the copy to be returned
	   * @return a copy of the original array, truncated or padded with zeros
	   *     to obtain the specified length
	   * @throws NegativeArraySizeException if <tt>newLength</tt> is negative
	   * @throws NullPointerException if <tt>original</tt> is null
	   * @since 1.6
	   */
	  public static long[] copyOf(long[] original, int newLength) {
		  long[] copy = new long[newLength];
		  System.arraycopy(original, 0, copy, 0,
				  Math.min(original.length, newLength));
		  return copy;
	  }

	  /**
	   * Copies the specified array, truncating or padding with null characters (if necessary)
	   * so the copy has the specified length.  For all indices that are valid
	   * in both the original array and the copy, the two arrays will contain
	   * identical values.  For any indices that are valid in the copy but not
	   * the original, the copy will contain <tt>'\\u000'</tt>.  Such indices
	   * will exist if and only if the specified length is greater than that of
	   * the original array.
	   *
	   * @param original the array to be copied
	   * @param newLength the length of the copy to be returned
	   * @return a copy of the original array, truncated or padded with null characters
	   *     to obtain the specified length
	   * @throws NegativeArraySizeException if <tt>newLength</tt> is negative
	   * @throws NullPointerException if <tt>original</tt> is null
	   * @since 1.6
	   */
	  public static char[] copyOf(char[] original, int newLength) {
		  char[] copy = new char[newLength];
		  System.arraycopy(original, 0, copy, 0,
				  Math.min(original.length, newLength));
		  return copy;
	  }

	  /**
	   * Copies the specified array, truncating or padding with zeros (if necessary)
	   * so the copy has the specified length.  For all indices that are
	   * valid in both the original array and the copy, the two arrays will
	   * contain identical values.  For any indices that are valid in the
	   * copy but not the original, the copy will contain <tt>0f</tt>.
	   * Such indices will exist if and only if the specified length
	   * is greater than that of the original array.
	   *
	   * @param original the array to be copied
	   * @param newLength the length of the copy to be returned
	   * @return a copy of the original array, truncated or padded with zeros
	   *     to obtain the specified length
	   * @throws NegativeArraySizeException if <tt>newLength</tt> is negative
	   * @throws NullPointerException if <tt>original</tt> is null
	   * @since 1.6
	   */
	  public static float[] copyOf(float[] original, int newLength) {
		  float[] copy = new float[newLength];
		  System.arraycopy(original, 0, copy, 0,
				  Math.min(original.length, newLength));
		  return copy;
	  }

	  /**
	   * Copies the specified array, truncating or padding with zeros (if necessary)
	   * so the copy has the specified length.  For all indices that are
	   * valid in both the original array and the copy, the two arrays will
	   * contain identical values.  For any indices that are valid in the
	   * copy but not the original, the copy will contain <tt>0d</tt>.
	   * Such indices will exist if and only if the specified length
	   * is greater than that of the original array.
	   *
	   * @param original the array to be copied
	   * @param newLength the length of the copy to be returned
	   * @return a copy of the original array, truncated or padded with zeros
	   *     to obtain the specified length
	   * @throws NegativeArraySizeException if <tt>newLength</tt> is negative
	   * @throws NullPointerException if <tt>original</tt> is null
	   * @since 1.6
	   */
	  public static double[] copyOf(double[] original, int newLength) {
		  double[] copy = new double[newLength];
		  System.arraycopy(original, 0, copy, 0,
				  Math.min(original.length, newLength));
		  return copy;
	  }

	  /**
	   * Copies the specified array, truncating or padding with <tt>false</tt> (if necessary)
	   * so the copy has the specified length.  For all indices that are
	   * valid in both the original array and the copy, the two arrays will
	   * contain identical values.  For any indices that are valid in the
	   * copy but not the original, the copy will contain <tt>false</tt>.
	   * Such indices will exist if and only if the specified length
	   * is greater than that of the original array.
	   *
	   * @param original the array to be copied
	   * @param newLength the length of the copy to be returned
	   * @return a copy of the original array, truncated or padded with false elements
	   *     to obtain the specified length
	   * @throws NegativeArraySizeException if <tt>newLength</tt> is negative
	   * @throws NullPointerException if <tt>original</tt> is null
	   * @since 1.6
	   */
	  public static boolean[] copyOf(boolean[] original, int newLength) {
		  boolean[] copy = new boolean[newLength];
		  System.arraycopy(original, 0, copy, 0,
				  Math.min(original.length, newLength));
		  return copy;
	  }

	  /**
	   * Copies the specified range of the specified array into a new array.
	   * The initial index of the range (<tt>from</tt>) must lie between zero
	   * and <tt>original.length</tt>, inclusive.  The value at
	   * <tt>original[from]</tt> is placed into the initial element of the copy
	   * (unless <tt>from == original.length</tt> or <tt>from == to</tt>).
	   * Values from subsequent elements in the original array are placed into
	   * subsequent elements in the copy.  The final index of the range
	   * (<tt>to</tt>), which must be greater than or equal to <tt>from</tt>,
	   * may be greater than <tt>original.length</tt>, in which case
	   * <tt>null</tt> is placed in all elements of the copy whose index is
	   * greater than or equal to <tt>original.length - from</tt>.  The length
	   * of the returned array will be <tt>to - from</tt>.
	   * <p>
	   * The resulting array is of exactly the same class as the original array.
	   *
	   * @param original the array from which a range is to be copied
	   * @param from the initial index of the range to be copied, inclusive
	   * @param to the final index of the range to be copied, exclusive.
	   *     (This index may lie outside the array.)
	   * @return a new array containing the specified range from the original array,
	   *     truncated or padded with nulls to obtain the required length
	   * @throws ArrayIndexOutOfBoundsException if <tt>from &lt; 0</tt>
	   *     or <tt>from &gt; original.length()</tt>
	   * @throws IllegalArgumentException if <tt>from &gt; to</tt>
	   * @throws NullPointerException if <tt>original</tt> is null
	   * @since 1.6
	   */
	  public static <T> T[] copyOfRange(T[] original, int from, int to) {
		  return copyOfRange(original, from, to, (Class<T[]>) original.getClass());
	  }

	  /**
	   * Copies the specified range of the specified array into a new array.
	   * The initial index of the range (<tt>from</tt>) must lie between zero
	   * and <tt>original.length</tt>, inclusive.  The value at
	   * <tt>original[from]</tt> is placed into the initial element of the copy
	   * (unless <tt>from == original.length</tt> or <tt>from == to</tt>).
	   * Values from subsequent elements in the original array are placed into
	   * subsequent elements in the copy.  The final index of the range
	   * (<tt>to</tt>), which must be greater than or equal to <tt>from</tt>,
	   * may be greater than <tt>original.length</tt>, in which case
	   * <tt>null</tt> is placed in all elements of the copy whose index is
	   * greater than or equal to <tt>original.length - from</tt>.  The length
	   * of the returned array will be <tt>to - from</tt>.
	   * The resulting array is of the class <tt>newType</tt>.
	   *
	   * @param original the array from which a range is to be copied
	   * @param from the initial index of the range to be copied, inclusive
	   * @param to the final index of the range to be copied, exclusive.
	   *     (This index may lie outside the array.)
	   * @param newType the class of the copy to be returned
	   * @return a new array containing the specified range from the original array,
	   *     truncated or padded with nulls to obtain the required length
	   * @throws ArrayIndexOutOfBoundsException if <tt>from &lt; 0</tt>
	   *     or <tt>from &gt; original.length()</tt>
	   * @throws IllegalArgumentException if <tt>from &gt; to</tt>
	   * @throws NullPointerException if <tt>original</tt> is null
	   * @throws ArrayStoreException if an element copied from
	   *     <tt>original</tt> is not of a runtime type that can be stored in
	   *     an array of class <tt>newType</tt>.
	   * @since 1.6
	   */
	  public static <T,U> T[] copyOfRange(U[] original, int from, int to, Class<? extends T[]> newType) {
		  int newLength = to - from;
		  if (newLength < 0)
			  throw new IllegalArgumentException(from + " > " + to);
		  T[] copy = ((Object)newType == (Object)Object[].class)
		  ? (T[]) new Object[newLength]
		                     : (T[]) Array.newInstance(newType.getComponentType(), newLength);
		  System.arraycopy(original, from, copy, 0,
				  Math.min(original.length - from, newLength));
		  return copy;
	  }

	  /**
	   * Copies the specified range of the specified array into a new array.
	   * The initial index of the range (<tt>from</tt>) must lie between zero
	   * and <tt>original.length</tt>, inclusive.  The value at
	   * <tt>original[from]</tt> is placed into the initial element of the copy
	   * (unless <tt>from == original.length</tt> or <tt>from == to</tt>).
	   * Values from subsequent elements in the original array are placed into
	   * subsequent elements in the copy.  The final index of the range
	   * (<tt>to</tt>), which must be greater than or equal to <tt>from</tt>,
	   * may be greater than <tt>original.length</tt>, in which case
	   * <tt>(byte)0</tt> is placed in all elements of the copy whose index is
	   * greater than or equal to <tt>original.length - from</tt>.  The length
	   * of the returned array will be <tt>to - from</tt>.
	   *
	   * @param original the array from which a range is to be copied
	   * @param from the initial index of the range to be copied, inclusive
	   * @param to the final index of the range to be copied, exclusive.
	   *     (This index may lie outside the array.)
	   * @return a new array containing the specified range from the original array,
	   *     truncated or padded with zeros to obtain the required length
	   * @throws ArrayIndexOutOfBoundsException if <tt>from &lt; 0</tt>
	   *     or <tt>from &gt; original.length()</tt>
	   * @throws IllegalArgumentException if <tt>from &gt; to</tt>
	   * @throws NullPointerException if <tt>original</tt> is null
	   * @since 1.6
	   */
	  public static byte[] copyOfRange(byte[] original, int from, int to) {
		  int newLength = to - from;
		  if (newLength < 0)
			  throw new IllegalArgumentException(from + " > " + to);
		  byte[] copy = new byte[newLength];
		  System.arraycopy(original, from, copy, 0,
				  Math.min(original.length - from, newLength));
		  return copy;
	  }

	  /**
	   * Copies the specified range of the specified array into a new array.
	   * The initial index of the range (<tt>from</tt>) must lie between zero
	   * and <tt>original.length</tt>, inclusive.  The value at
	   * <tt>original[from]</tt> is placed into the initial element of the copy
	   * (unless <tt>from == original.length</tt> or <tt>from == to</tt>).
	   * Values from subsequent elements in the original array are placed into
	   * subsequent elements in the copy.  The final index of the range
	   * (<tt>to</tt>), which must be greater than or equal to <tt>from</tt>,
	   * may be greater than <tt>original.length</tt>, in which case
	   * <tt>(short)0</tt> is placed in all elements of the copy whose index is
	   * greater than or equal to <tt>original.length - from</tt>.  The length
	   * of the returned array will be <tt>to - from</tt>.
	   *
	   * @param original the array from which a range is to be copied
	   * @param from the initial index of the range to be copied, inclusive
	   * @param to the final index of the range to be copied, exclusive.
	   *     (This index may lie outside the array.)
	   * @return a new array containing the specified range from the original array,
	   *     truncated or padded with zeros to obtain the required length
	   * @throws ArrayIndexOutOfBoundsException if <tt>from &lt; 0</tt>
	   *     or <tt>from &gt; original.length()</tt>
	   * @throws IllegalArgumentException if <tt>from &gt; to</tt>
	   * @throws NullPointerException if <tt>original</tt> is null
	   * @since 1.6
	   */
	  public static short[] copyOfRange(short[] original, int from, int to) {
		  int newLength = to - from;
		  if (newLength < 0)
			  throw new IllegalArgumentException(from + " > " + to);
		  short[] copy = new short[newLength];
		  System.arraycopy(original, from, copy, 0,
				  Math.min(original.length - from, newLength));
		  return copy;
	  }

	  /**
	   * Copies the specified range of the specified array into a new array.
	   * The initial index of the range (<tt>from</tt>) must lie between zero
	   * and <tt>original.length</tt>, inclusive.  The value at
	   * <tt>original[from]</tt> is placed into the initial element of the copy
	   * (unless <tt>from == original.length</tt> or <tt>from == to</tt>).
	   * Values from subsequent elements in the original array are placed into
	   * subsequent elements in the copy.  The final index of the range
	   * (<tt>to</tt>), which must be greater than or equal to <tt>from</tt>,
	   * may be greater than <tt>original.length</tt>, in which case
	   * <tt>0</tt> is placed in all elements of the copy whose index is
	   * greater than or equal to <tt>original.length - from</tt>.  The length
	   * of the returned array will be <tt>to - from</tt>.
	   *
	   * @param original the array from which a range is to be copied
	   * @param from the initial index of the range to be copied, inclusive
	   * @param to the final index of the range to be copied, exclusive.
	   *     (This index may lie outside the array.)
	   * @return a new array containing the specified range from the original array,
	   *     truncated or padded with zeros to obtain the required length
	   * @throws ArrayIndexOutOfBoundsException if <tt>from &lt; 0</tt>
	   *     or <tt>from &gt; original.length()</tt>
	   * @throws IllegalArgumentException if <tt>from &gt; to</tt>
	   * @throws NullPointerException if <tt>original</tt> is null
	   * @since 1.6
	   */
	  public static int[] copyOfRange(int[] original, int from, int to) {
		  int newLength = to - from;
		  if (newLength < 0)
			  throw new IllegalArgumentException(from + " > " + to);
		  int[] copy = new int[newLength];
		  System.arraycopy(original, from, copy, 0,
				  Math.min(original.length - from, newLength));
		  return copy;
	  }

	  /**
	   * Copies the specified range of the specified array into a new array.
	   * The initial index of the range (<tt>from</tt>) must lie between zero
	   * and <tt>original.length</tt>, inclusive.  The value at
	   * <tt>original[from]</tt> is placed into the initial element of the copy
	   * (unless <tt>from == original.length</tt> or <tt>from == to</tt>).
	   * Values from subsequent elements in the original array are placed into
	   * subsequent elements in the copy.  The final index of the range
	   * (<tt>to</tt>), which must be greater than or equal to <tt>from</tt>,
	   * may be greater than <tt>original.length</tt>, in which case
	   * <tt>0L</tt> is placed in all elements of the copy whose index is
	   * greater than or equal to <tt>original.length - from</tt>.  The length
	   * of the returned array will be <tt>to - from</tt>.
	   *
	   * @param original the array from which a range is to be copied
	   * @param from the initial index of the range to be copied, inclusive
	   * @param to the final index of the range to be copied, exclusive.
	   *     (This index may lie outside the array.)
	   * @return a new array containing the specified range from the original array,
	   *     truncated or padded with zeros to obtain the required length
	   * @throws ArrayIndexOutOfBoundsException if <tt>from &lt; 0</tt>
	   *     or <tt>from &gt; original.length()</tt>
	   * @throws IllegalArgumentException if <tt>from &gt; to</tt>
	   * @throws NullPointerException if <tt>original</tt> is null
	   * @since 1.6
	   */
	  public static long[] copyOfRange(long[] original, int from, int to) {
		  int newLength = to - from;
		  if (newLength < 0)
			  throw new IllegalArgumentException(from + " > " + to);
		  long[] copy = new long[newLength];
		  System.arraycopy(original, from, copy, 0,
				  Math.min(original.length - from, newLength));
		  return copy;
	  }

	  /**
	   * Copies the specified range of the specified array into a new array.
	   * The initial index of the range (<tt>from</tt>) must lie between zero
	   * and <tt>original.length</tt>, inclusive.  The value at
	   * <tt>original[from]</tt> is placed into the initial element of the copy
	   * (unless <tt>from == original.length</tt> or <tt>from == to</tt>).
	   * Values from subsequent elements in the original array are placed into
	   * subsequent elements in the copy.  The final index of the range
	   * (<tt>to</tt>), which must be greater than or equal to <tt>from</tt>,
	   * may be greater than <tt>original.length</tt>, in which case
	   * <tt>'\\u000'</tt> is placed in all elements of the copy whose index is
	   * greater than or equal to <tt>original.length - from</tt>.  The length
	   * of the returned array will be <tt>to - from</tt>.
	   *
	   * @param original the array from which a range is to be copied
	   * @param from the initial index of the range to be copied, inclusive
	   * @param to the final index of the range to be copied, exclusive.
	   *     (This index may lie outside the array.)
	   * @return a new array containing the specified range from the original array,
	   *     truncated or padded with null characters to obtain the required length
	   * @throws ArrayIndexOutOfBoundsException if <tt>from &lt; 0</tt>
	   *     or <tt>from &gt; original.length()</tt>
	   * @throws IllegalArgumentException if <tt>from &gt; to</tt>
	   * @throws NullPointerException if <tt>original</tt> is null
	   * @since 1.6
	   */
	  public static char[] copyOfRange(char[] original, int from, int to) {
		  int newLength = to - from;
		  if (newLength < 0)
			  throw new IllegalArgumentException(from + " > " + to);
		  char[] copy = new char[newLength];
		  System.arraycopy(original, from, copy, 0,
				  Math.min(original.length - from, newLength));
		  return copy;
	  }

	  /**
	   * Copies the specified range of the specified array into a new array.
	   * The initial index of the range (<tt>from</tt>) must lie between zero
	   * and <tt>original.length</tt>, inclusive.  The value at
	   * <tt>original[from]</tt> is placed into the initial element of the copy
	   * (unless <tt>from == original.length</tt> or <tt>from == to</tt>).
	   * Values from subsequent elements in the original array are placed into
	   * subsequent elements in the copy.  The final index of the range
	   * (<tt>to</tt>), which must be greater than or equal to <tt>from</tt>,
	   * may be greater than <tt>original.length</tt>, in which case
	   * <tt>0f</tt> is placed in all elements of the copy whose index is
	   * greater than or equal to <tt>original.length - from</tt>.  The length
	   * of the returned array will be <tt>to - from</tt>.
	   *
	   * @param original the array from which a range is to be copied
	   * @param from the initial index of the range to be copied, inclusive
	   * @param to the final index of the range to be copied, exclusive.
	   *     (This index may lie outside the array.)
	   * @return a new array containing the specified range from the original array,
	   *     truncated or padded with zeros to obtain the required length
	   * @throws ArrayIndexOutOfBoundsException if <tt>from &lt; 0</tt>
	   *     or <tt>from &gt; original.length()</tt>
	   * @throws IllegalArgumentException if <tt>from &gt; to</tt>
	   * @throws NullPointerException if <tt>original</tt> is null
	   * @since 1.6
	   */
	  public static float[] copyOfRange(float[] original, int from, int to) {
		  int newLength = to - from;
		  if (newLength < 0)
			  throw new IllegalArgumentException(from + " > " + to);
		  float[] copy = new float[newLength];
		  System.arraycopy(original, from, copy, 0,
				  Math.min(original.length - from, newLength));
		  return copy;
	  }

	  /**
	   * Copies the specified range of the specified array into a new array.
	   * The initial index of the range (<tt>from</tt>) must lie between zero
	   * and <tt>original.length</tt>, inclusive.  The value at
	   * <tt>original[from]</tt> is placed into the initial element of the copy
	   * (unless <tt>from == original.length</tt> or <tt>from == to</tt>).
	   * Values from subsequent elements in the original array are placed into
	   * subsequent elements in the copy.  The final index of the range
	   * (<tt>to</tt>), which must be greater than or equal to <tt>from</tt>,
	   * may be greater than <tt>original.length</tt>, in which case
	   * <tt>0d</tt> is placed in all elements of the copy whose index is
	   * greater than or equal to <tt>original.length - from</tt>.  The length
	   * of the returned array will be <tt>to - from</tt>.
	   *
	   * @param original the array from which a range is to be copied
	   * @param from the initial index of the range to be copied, inclusive
	   * @param to the final index of the range to be copied, exclusive.
	   *     (This index may lie outside the array.)
	   * @return a new array containing the specified range from the original array,
	   *     truncated or padded with zeros to obtain the required length
	   * @throws ArrayIndexOutOfBoundsException if <tt>from &lt; 0</tt>
	   *     or <tt>from &gt; original.length()</tt>
	   * @throws IllegalArgumentException if <tt>from &gt; to</tt>
	   * @throws NullPointerException if <tt>original</tt> is null
	   * @since 1.6
	   */
	  public static double[] copyOfRange(double[] original, int from, int to) {
		  int newLength = to - from;
		  if (newLength < 0)
			  throw new IllegalArgumentException(from + " > " + to);
		  double[] copy = new double[newLength];
		  System.arraycopy(original, from, copy, 0,
				  Math.min(original.length - from, newLength));
		  return copy;
	  }

	  /**
	   * Copies the specified range of the specified array into a new array.
	   * The initial index of the range (<tt>from</tt>) must lie between zero
	   * and <tt>original.length</tt>, inclusive.  The value at
	   * <tt>original[from]</tt> is placed into the initial element of the copy
	   * (unless <tt>from == original.length</tt> or <tt>from == to</tt>).
	   * Values from subsequent elements in the original array are placed into
	   * subsequent elements in the copy.  The final index of the range
	   * (<tt>to</tt>), which must be greater than or equal to <tt>from</tt>,
	   * may be greater than <tt>original.length</tt>, in which case
	   * <tt>false</tt> is placed in all elements of the copy whose index is
	   * greater than or equal to <tt>original.length - from</tt>.  The length
	   * of the returned array will be <tt>to - from</tt>.
	   *
	   * @param original the array from which a range is to be copied
	   * @param from the initial index of the range to be copied, inclusive
	   * @param to the final index of the range to be copied, exclusive.
	   *     (This index may lie outside the array.)
	   * @return a new array containing the specified range from the original array,
	   *     truncated or padded with false elements to obtain the required length
	   * @throws ArrayIndexOutOfBoundsException if <tt>from &lt; 0</tt>
	   *     or <tt>from &gt; original.length()</tt>
	   * @throws IllegalArgumentException if <tt>from &gt; to</tt>
	   * @throws NullPointerException if <tt>original</tt> is null
	   * @since 1.6
	   */
	  public static boolean[] copyOfRange(boolean[] original, int from, int to) {
		  int newLength = to - from;
		  if (newLength < 0)
			  throw new IllegalArgumentException(from + " > " + to);
		  boolean[] copy = new boolean[newLength];
		  System.arraycopy(original, from, copy, 0,
				  Math.min(original.length - from, newLength));
		  return copy;
	  }


	  // Misc

	  /**
	   * Returns a fixed-size list backed by the specified array.  (Changes to
	   * the returned list "write through" to the array.)  This method acts
	   * as bridge between array-based and collection-based APIs, in
	   * combination with {@link Collection#toArray}.  The returned list is
	   * serializable and implements {@link RandomAccess}.
	   *
	   * <p>This method also provides a convenient way to create a fixed-size
	   * list initialized to contain several elements:
	   * <pre>
	   *     List&lt;String&gt; stooges = Arrays.asList("Larry", "Moe", "Curly");
	   * </pre>
	   *
	   * @param a the array by which the list will be backed
	   * @return a list view of the specified array
	   */
	  public static <T> List<T> asList(T... a) {
		  return new _ArrayList<T>(a);
	  }

	  /**
	   * @serial include
	   */
	  private static class _ArrayList<E> extends AbstractList<E>
	  implements RandomAccess, java.io.Serializable
	  {
		  private static final long serialVersionUID = -2764017481108945198L;
		  private final E[] a;

		  _ArrayList(E[] array) {
			  if (array==null)
				  throw new NullPointerException();
			  a = array;
		  }

		  public int size() {
			  return a.length;
		  }

		  public Object[] toArray() {
			  return a.clone();
		  }

		  public <T> T[] toArray(T[] a) {
			  int size = size();
			  if (a.length < size)
				  return Arrays.copyOf(this.a, size,
						  (Class<? extends T[]>) a.getClass());
			  System.arraycopy(this.a, 0, a, 0, size);
			  if (a.length > size)
				  a[size] = null;
			  return a;
		  }

		  public E get(int index) {
			  return a[index];
		  }

		  public E set(int index, E element) {
			  E oldValue = a[index];
			  a[index] = element;
			  return oldValue;
		  }

		  public int indexOf(Object o) {
			  if (o==null) {
				  for (int i=0; i<a.length; i++)
					  if (a[i]==null)
						  return i;
			  } else {
				  for (int i=0; i<a.length; i++)
					  if (o.equals(a[i]))
						  return i;
			  }
			  return -1;
		  }

		  public boolean contains(Object o) {
			  return indexOf(o) != -1;
		  }
	  }

	  /**
	   * Returns a hash code based on the contents of the specified array.
	   * For any two <tt>long</tt> arrays <tt>a</tt> and <tt>b</tt>
	   * such that <tt>Arrays.equals(a, b)</tt>, it is also the case that
	   * <tt>Arrays.hashCode(a) == Arrays.hashCode(b)</tt>.
	   *
	   * <p>The value returned by this method is the same value that would be
	   * obtained by invoking the {@link List#hashCode() <tt>hashCode</tt>}
	   * method on a {@link List} containing a sequence of {@link Long}
	   * instances representing the elements of <tt>a</tt> in the same order.
	   * If <tt>a</tt> is <tt>null</tt>, this method returns 0.
	   *
	   * @param a the array whose hash value to compute
	   * @return a content-based hash code for <tt>a</tt>
	   * @since 1.5
	   */
	  public static int hashCode(long a[]) {
		  if (a == null)
			  return 0;

		  int result = 1;
		  for (long element : a) {
			  int elementHash = (int)(element ^ (element >>> 32));
			  result = 31 * result + elementHash;
		  }

		  return result;
	  }

	  /**
	   * Returns a hash code based on the contents of the specified array.
	   * For any two non-null <tt>int</tt> arrays <tt>a</tt> and <tt>b</tt>
	   * such that <tt>Arrays.equals(a, b)</tt>, it is also the case that
	   * <tt>Arrays.hashCode(a) == Arrays.hashCode(b)</tt>.
	   *
	   * <p>The value returned by this method is the same value that would be
	   * obtained by invoking the {@link List#hashCode() <tt>hashCode</tt>}
	   * method on a {@link List} containing a sequence of {@link Integer}
	   * instances representing the elements of <tt>a</tt> in the same order.
	   * If <tt>a</tt> is <tt>null</tt>, this method returns 0.
	   *
	   * @param a the array whose hash value to compute
	   * @return a content-based hash code for <tt>a</tt>
	   * @since 1.5
	   */
	  public static int hashCode(int a[]) {
		  if (a == null)
			  return 0;

		  int result = 1;
		  for (int element : a)
			  result = 31 * result + element;

		  return result;
	  }

	  /**
	   * Returns a hash code based on the contents of the specified array.
	   * For any two <tt>short</tt> arrays <tt>a</tt> and <tt>b</tt>
	   * such that <tt>Arrays.equals(a, b)</tt>, it is also the case that
	   * <tt>Arrays.hashCode(a) == Arrays.hashCode(b)</tt>.
	   *
	   * <p>The value returned by this method is the same value that would be
	   * obtained by invoking the {@link List#hashCode() <tt>hashCode</tt>}
	   * method on a {@link List} containing a sequence of {@link Short}
	   * instances representing the elements of <tt>a</tt> in the same order.
	   * If <tt>a</tt> is <tt>null</tt>, this method returns 0.
	   *
	   * @param a the array whose hash value to compute
	   * @return a content-based hash code for <tt>a</tt>
	   * @since 1.5
	   */
	  public static int hashCode(short a[]) {
		  if (a == null)
			  return 0;

		  int result = 1;
		  for (short element : a)
			  result = 31 * result + element;

		  return result;
	  }

	  /**
	   * Returns a hash code based on the contents of the specified array.
	   * For any two <tt>char</tt> arrays <tt>a</tt> and <tt>b</tt>
	   * such that <tt>Arrays.equals(a, b)</tt>, it is also the case that
	   * <tt>Arrays.hashCode(a) == Arrays.hashCode(b)</tt>.
	   *
	   * <p>The value returned by this method is the same value that would be
	   * obtained by invoking the {@link List#hashCode() <tt>hashCode</tt>}
	   * method on a {@link List} containing a sequence of {@link Character}
	   * instances representing the elements of <tt>a</tt> in the same order.
	   * If <tt>a</tt> is <tt>null</tt>, this method returns 0.
	   *
	   * @param a the array whose hash value to compute
	   * @return a content-based hash code for <tt>a</tt>
	   * @since 1.5
	   */
	  public static int hashCode(char a[]) {
		  if (a == null)
			  return 0;

		  int result = 1;
		  for (char element : a)
			  result = 31 * result + element;

		  return result;
	  }

	  /**
	   * Returns a hash code based on the contents of the specified array.
	   * For any two <tt>byte</tt> arrays <tt>a</tt> and <tt>b</tt>
	   * such that <tt>Arrays.equals(a, b)</tt>, it is also the case that
	   * <tt>Arrays.hashCode(a) == Arrays.hashCode(b)</tt>.
	   *
	   * <p>The value returned by this method is the same value that would be
	   * obtained by invoking the {@link List#hashCode() <tt>hashCode</tt>}
	   * method on a {@link List} containing a sequence of {@link Byte}
	   * instances representing the elements of <tt>a</tt> in the same order.
	   * If <tt>a</tt> is <tt>null</tt>, this method returns 0.
	   *
	   * @param a the array whose hash value to compute
	   * @return a content-based hash code for <tt>a</tt>
	   * @since 1.5
	   */
	  public static int hashCode(byte a[]) {
		  if (a == null)
			  return 0;

		  int result = 1;
		  for (byte element : a)
			  result = 31 * result + element;

		  return result;
	  }

	  /**
	   * Returns a hash code based on the contents of the specified array.
	   * For any two <tt>boolean</tt> arrays <tt>a</tt> and <tt>b</tt>
	   * such that <tt>Arrays.equals(a, b)</tt>, it is also the case that
	   * <tt>Arrays.hashCode(a) == Arrays.hashCode(b)</tt>.
	   *
	   * <p>The value returned by this method is the same value that would be
	   * obtained by invoking the {@link List#hashCode() <tt>hashCode</tt>}
	   * method on a {@link List} containing a sequence of {@link Boolean}
	   * instances representing the elements of <tt>a</tt> in the same order.
	   * If <tt>a</tt> is <tt>null</tt>, this method returns 0.
	   *
	   * @param a the array whose hash value to compute
	   * @return a content-based hash code for <tt>a</tt>
	   * @since 1.5
	   */
	  public static int hashCode(boolean a[]) {
		  if (a == null)
			  return 0;

		  int result = 1;
		  for (boolean element : a)
			  result = 31 * result + (element ? 1231 : 1237);

		  return result;
	  }

	  /**
	   * Returns a hash code based on the contents of the specified array.
	   * For any two <tt>float</tt> arrays <tt>a</tt> and <tt>b</tt>
	   * such that <tt>Arrays.equals(a, b)</tt>, it is also the case that
	   * <tt>Arrays.hashCode(a) == Arrays.hashCode(b)</tt>.
	   *
	   * <p>The value returned by this method is the same value that would be
	   * obtained by invoking the {@link List#hashCode() <tt>hashCode</tt>}
	   * method on a {@link List} containing a sequence of {@link Float}
	   * instances representing the elements of <tt>a</tt> in the same order.
	   * If <tt>a</tt> is <tt>null</tt>, this method returns 0.
	   *
	   * @param a the array whose hash value to compute
	   * @return a content-based hash code for <tt>a</tt>
	   * @since 1.5
	   */
	  public static int hashCode(float a[]) {
		  if (a == null)
			  return 0;

		  int result = 1;
		  for (float element : a)
			  result = 31 * result + Float.floatToIntBits(element);

		  return result;
	  }

	  /**
	   * Returns a hash code based on the contents of the specified array.
	   * For any two <tt>double</tt> arrays <tt>a</tt> and <tt>b</tt>
	   * such that <tt>Arrays.equals(a, b)</tt>, it is also the case that
	   * <tt>Arrays.hashCode(a) == Arrays.hashCode(b)</tt>.
	   *
	   * <p>The value returned by this method is the same value that would be
	   * obtained by invoking the {@link List#hashCode() <tt>hashCode</tt>}
	   * method on a {@link List} containing a sequence of {@link Double}
	   * instances representing the elements of <tt>a</tt> in the same order.
	   * If <tt>a</tt> is <tt>null</tt>, this method returns 0.
	   *
	   * @param a the array whose hash value to compute
	   * @return a content-based hash code for <tt>a</tt>
	   * @since 1.5
	   */
	  public static int hashCode(double a[]) {
		  if (a == null)
			  return 0;

		  int result = 1;
		  for (double element : a) {
			  long bits = Double.doubleToLongBits(element);
			  result = 31 * result + (int)(bits ^ (bits >>> 32));
		  }
		  return result;
	  }

	  /**
	   * Returns a hash code based on the contents of the specified array.  If
	   * the array contains other arrays as elements, the hash code is based on
	   * their identities rather than their contents.  It is therefore
	   * acceptable to invoke this method on an array that contains itself as an
	   * element,  either directly or indirectly through one or more levels of
	   * arrays.
	   *
	   * <p>For any two arrays <tt>a</tt> and <tt>b</tt> such that
	   * <tt>Arrays.equals(a, b)</tt>, it is also the case that
	   * <tt>Arrays.hashCode(a) == Arrays.hashCode(b)</tt>.
	   *
	   * <p>The value returned by this method is equal to the value that would
	   * be returned by <tt>Arrays.asList(a).hashCode()</tt>, unless <tt>a</tt>
	   * is <tt>null</tt>, in which case <tt>0</tt> is returned.
	   *
	   * @param a the array whose content-based hash code to compute
	   * @return a content-based hash code for <tt>a</tt>
	   * @see #deepHashCode(Object[])
	   * @since 1.5
	   */
	  public static int hashCode(Object a[]) {
		  if (a == null)
			  return 0;

		  int result = 1;

		  for (Object element : a)
			  result = 31 * result + (element == null ? 0 : element.hashCode());

		  return result;
	  }

	  /**
	   * Returns a hash code based on the "deep contents" of the specified
	   * array.  If the array contains other arrays as elements, the
	   * hash code is based on their contents and so on, ad infinitum.
	   * It is therefore unacceptable to invoke this method on an array that
	   * contains itself as an element, either directly or indirectly through
	   * one or more levels of arrays.  The behavior of such an invocation is
	   * undefined.
	   *
	   * <p>For any two arrays <tt>a</tt> and <tt>b</tt> such that
	   * <tt>Arrays.deepEquals(a, b)</tt>, it is also the case that
	   * <tt>Arrays.deepHashCode(a) == Arrays.deepHashCode(b)</tt>.
	   *
	   * <p>The computation of the value returned by this method is similar to
	   * that of the value returned by {@link List#hashCode()} on a list
	   * containing the same elements as <tt>a</tt> in the same order, with one
	   * difference: If an element <tt>e</tt> of <tt>a</tt> is itself an array,
	   * its hash code is computed not by calling <tt>e.hashCode()</tt>, but as
	   * by calling the appropriate overloading of <tt>Arrays.hashCode(e)</tt>
	   * if <tt>e</tt> is an array of a primitive type, or as by calling
	   * <tt>Arrays.deepHashCode(e)</tt> recursively if <tt>e</tt> is an array
	   * of a reference type.  If <tt>a</tt> is <tt>null</tt>, this method
	   * returns 0.
	   *
	   * @param a the array whose deep-content-based hash code to compute
	   * @return a deep-content-based hash code for <tt>a</tt>
	   * @see #hashCode(Object[])
	   * @since 1.5
	   */
	  public static int deepHashCode(Object a[]) {
		  if (a == null)
			  return 0;

		  int result = 1;

		  for (Object element : a) {
			  int elementHash = 0;
			  if (element instanceof Object[])
				  elementHash = deepHashCode((Object[]) element);
			  else if (element instanceof byte[])
				  elementHash = hashCode((byte[]) element);
			  else if (element instanceof short[])
				  elementHash = hashCode((short[]) element);
			  else if (element instanceof int[])
				  elementHash = hashCode((int[]) element);
			  else if (element instanceof long[])
				  elementHash = hashCode((long[]) element);
			  else if (element instanceof char[])
				  elementHash = hashCode((char[]) element);
			  else if (element instanceof float[])
				  elementHash = hashCode((float[]) element);
			  else if (element instanceof double[])
				  elementHash = hashCode((double[]) element);
			  else if (element instanceof boolean[])
				  elementHash = hashCode((boolean[]) element);
			  else if (element != null)
				  elementHash = element.hashCode();

			  result = 31 * result + elementHash;
		  }

		  return result;
	  }

	  /**
	   * Returns <tt>true</tt> if the two specified arrays are <i>deeply
	   * equal</i> to one another.  Unlike the {@link #equals(Object[],Object[])}
	   * method, this method is appropriate for use with nested arrays of
	   * arbitrary depth.
	   *
	   * <p>Two array references are considered deeply equal if both
	   * are <tt>null</tt>, or if they refer to arrays that contain the same
	   * number of elements and all corresponding pairs of elements in the two
	   * arrays are deeply equal.
	   *
	   * <p>Two possibly <tt>null</tt> elements <tt>e1</tt> and <tt>e2</tt> are
	   * deeply equal if any of the following conditions hold:
	   * <ul>
	   *    <li> <tt>e1</tt> and <tt>e2</tt> are both arrays of object reference
	   *         types, and <tt>Arrays.deepEquals(e1, e2) would return true</tt>
	   *    <li> <tt>e1</tt> and <tt>e2</tt> are arrays of the same primitive
	   *         type, and the appropriate overloading of
	   *         <tt>Arrays.equals(e1, e2)</tt> would return true.
	   *    <li> <tt>e1 == e2</tt>
	   *    <li> <tt>e1.equals(e2)</tt> would return true.
	   * </ul>
	   * Note that this definition permits <tt>null</tt> elements at any depth.
	   *
	   * <p>If either of the specified arrays contain themselves as elements
	   * either directly or indirectly through one or more levels of arrays,
	   * the behavior of this method is undefined.
	   *
	   * @param a1 one array to be tested for equality
	   * @param a2 the other array to be tested for equality
	   * @return <tt>true</tt> if the two arrays are equal
	   * @see #equals(Object[],Object[])
	   * @since 1.5
	   */
	  public static boolean deepEquals(Object[] a1, Object[] a2) {
		  if (a1 == a2)
			  return true;
		  if (a1 == null || a2==null)
			  return false;
		  int length = a1.length;
		  if (a2.length != length)
			  return false;

		  for (int i = 0; i < length; i++) {
			  Object e1 = a1[i];
			  Object e2 = a2[i];

			  if (e1 == e2)
				  continue;
			  if (e1 == null)
				  return false;

			  // Figure out whether the two elements are equal
			  boolean eq;
			  if (e1 instanceof Object[] && e2 instanceof Object[])
				  eq = deepEquals ((Object[]) e1, (Object[]) e2);
			  else if (e1 instanceof byte[] && e2 instanceof byte[])
				  eq = equals((byte[]) e1, (byte[]) e2);
			  else if (e1 instanceof short[] && e2 instanceof short[])
				  eq = equals((short[]) e1, (short[]) e2);
			  else if (e1 instanceof int[] && e2 instanceof int[])
				  eq = equals((int[]) e1, (int[]) e2);
			  else if (e1 instanceof long[] && e2 instanceof long[])
				  eq = equals((long[]) e1, (long[]) e2);
			  else if (e1 instanceof char[] && e2 instanceof char[])
				  eq = equals((char[]) e1, (char[]) e2);
			  else if (e1 instanceof float[] && e2 instanceof float[])
				  eq = equals((float[]) e1, (float[]) e2);
			  else if (e1 instanceof double[] && e2 instanceof double[])
				  eq = equals((double[]) e1, (double[]) e2);
			  else if (e1 instanceof boolean[] && e2 instanceof boolean[])
				  eq = equals((boolean[]) e1, (boolean[]) e2);
			  else
				  eq = e1.equals(e2);

			  if (!eq)
				  return false;
		  }
		  return true;
	  }

	  /**
	   * Returns a string representation of the contents of the specified array.
	   * The string representation consists of a list of the array's elements,
	   * enclosed in square brackets (<tt>"[]"</tt>).  Adjacent elements are
	   * separated by the characters <tt>", "</tt> (a comma followed by a
	   * space).  Elements are converted to strings as by
	   * <tt>String.valueOf(long)</tt>.  Returns <tt>"null"</tt> if <tt>a</tt>
	   * is <tt>null</tt>.
	   *
	   * @param a the array whose string representation to return
	   * @return a string representation of <tt>a</tt>
	   * @since 1.5
	   */
	  public static String toString(long[] a) {
		  if (a == null)
			  return "null";
		  int iMax = a.length - 1;
		  if (iMax == -1)
			  return "[]";

		  StringBuilder b = new StringBuilder();
		  b.append('[');
		  for (int i = 0; ; i++) {
			  b.append(a[i]);
			  if (i == iMax)
				  return b.append(']').toString();
			  b.append(", ");
		  }
	  }

	  /**
	   * Returns a string representation of the contents of the specified array.
	   * The string representation consists of a list of the array's elements,
	   * enclosed in square brackets (<tt>"[]"</tt>).  Adjacent elements are
	   * separated by the characters <tt>", "</tt> (a comma followed by a
	   * space).  Elements are converted to strings as by
	   * <tt>String.valueOf(int)</tt>.  Returns <tt>"null"</tt> if <tt>a</tt> is
	   * <tt>null</tt>.
	   *
	   * @param a the array whose string representation to return
	   * @return a string representation of <tt>a</tt>
	   * @since 1.5
	   */
	  public static String toString(int[] a) {
		  if (a == null)
			  return "null";
		  int iMax = a.length - 1;
		  if (iMax == -1)
			  return "[]";

		  StringBuilder b = new StringBuilder();
		  b.append('[');
		  for (int i = 0; ; i++) {
			  b.append(a[i]);
			  if (i == iMax)
				  return b.append(']').toString();
			  b.append(", ");
		  }
	  }

	  /**
	   * Returns a string representation of the contents of the specified array.
	   * The string representation consists of a list of the array's elements,
	   * enclosed in square brackets (<tt>"[]"</tt>).  Adjacent elements are
	   * separated by the characters <tt>", "</tt> (a comma followed by a
	   * space).  Elements are converted to strings as by
	   * <tt>String.valueOf(short)</tt>.  Returns <tt>"null"</tt> if <tt>a</tt>
	   * is <tt>null</tt>.
	   *
	   * @param a the array whose string representation to return
	   * @return a string representation of <tt>a</tt>
	   * @since 1.5
	   */
	  public static String toString(short[] a) {
		  if (a == null)
			  return "null";
		  int iMax = a.length - 1;
		  if (iMax == -1)
			  return "[]";

		  StringBuilder b = new StringBuilder();
		  b.append('[');
		  for (int i = 0; ; i++) {
			  b.append(a[i]);
			  if (i == iMax)
				  return b.append(']').toString();
			  b.append(", ");
		  }
	  }

	  /**
	   * Returns a string representation of the contents of the specified array.
	   * The string representation consists of a list of the array's elements,
	   * enclosed in square brackets (<tt>"[]"</tt>).  Adjacent elements are
	   * separated by the characters <tt>", "</tt> (a comma followed by a
	   * space).  Elements are converted to strings as by
	   * <tt>String.valueOf(char)</tt>.  Returns <tt>"null"</tt> if <tt>a</tt>
	   * is <tt>null</tt>.
	   *
	   * @param a the array whose string representation to return
	   * @return a string representation of <tt>a</tt>
	   * @since 1.5
	   */
	  public static String toString(char[] a) {
		  if (a == null)
			  return "null";
		  int iMax = a.length - 1;
		  if (iMax == -1)
			  return "[]";

		  StringBuilder b = new StringBuilder();
		  b.append('[');
		  for (int i = 0; ; i++) {
			  b.append(a[i]);
			  if (i == iMax)
				  return b.append(']').toString();
			  b.append(", ");
		  }
	  }

	  /**
	   * Returns a string representation of the contents of the specified array.
	   * The string representation consists of a list of the array's elements,
	   * enclosed in square brackets (<tt>"[]"</tt>).  Adjacent elements
	   * are separated by the characters <tt>", "</tt> (a comma followed
	   * by a space).  Elements are converted to strings as by
	   * <tt>String.valueOf(byte)</tt>.  Returns <tt>"null"</tt> if
	   * <tt>a</tt> is <tt>null</tt>.
	   *
	   * @param a the array whose string representation to return
	   * @return a string representation of <tt>a</tt>
	   * @since 1.5
	   */
	  public static String toString(byte[] a) {
		  if (a == null)
			  return "null";
		  int iMax = a.length - 1;
		  if (iMax == -1)
			  return "[]";

		  StringBuilder b = new StringBuilder();
		  b.append('[');
		  for (int i = 0; ; i++) {
			  b.append(a[i]);
			  if (i == iMax)
				  return b.append(']').toString();
			  b.append(", ");
		  }
	  }

	  /**
	   * Returns a string representation of the contents of the specified array.
	   * The string representation consists of a list of the array's elements,
	   * enclosed in square brackets (<tt>"[]"</tt>).  Adjacent elements are
	   * separated by the characters <tt>", "</tt> (a comma followed by a
	   * space).  Elements are converted to strings as by
	   * <tt>String.valueOf(boolean)</tt>.  Returns <tt>"null"</tt> if
	   * <tt>a</tt> is <tt>null</tt>.
	   *
	   * @param a the array whose string representation to return
	   * @return a string representation of <tt>a</tt>
	   * @since 1.5
	   */
	  public static String toString(boolean[] a) {
		  if (a == null)
			  return "null";
		  int iMax = a.length - 1;
		  if (iMax == -1)
			  return "[]";

		  StringBuilder b = new StringBuilder();
		  b.append('[');
		  for (int i = 0; ; i++) {
			  b.append(a[i]);
			  if (i == iMax)
				  return b.append(']').toString();
			  b.append(", ");
		  }
	  }

	  /**
	   * Returns a string representation of the contents of the specified array.
	   * The string representation consists of a list of the array's elements,
	   * enclosed in square brackets (<tt>"[]"</tt>).  Adjacent elements are
	   * separated by the characters <tt>", "</tt> (a comma followed by a
	   * space).  Elements are converted to strings as by
	   * <tt>String.valueOf(float)</tt>.  Returns <tt>"null"</tt> if <tt>a</tt>
	   * is <tt>null</tt>.
	   *
	   * @param a the array whose string representation to return
	   * @return a string representation of <tt>a</tt>
	   * @since 1.5
	   */
	  public static String toString(float[] a) {
		  if (a == null)
			  return "null";
		  int iMax = a.length - 1;
		  if (iMax == -1)
			  return "[]";

		  StringBuilder b = new StringBuilder();
		  b.append('[');
		  for (int i = 0; ; i++) {
			  b.append(a[i]);
			  if (i == iMax)
				  return b.append(']').toString();
			  b.append(", ");
		  }
	  }

	  /**
	   * Returns a string representation of the contents of the specified array.
	   * The string representation consists of a list of the array's elements,
	   * enclosed in square brackets (<tt>"[]"</tt>).  Adjacent elements are
	   * separated by the characters <tt>", "</tt> (a comma followed by a
	   * space).  Elements are converted to strings as by
	   * <tt>String.valueOf(double)</tt>.  Returns <tt>"null"</tt> if <tt>a</tt>
	   * is <tt>null</tt>.
	   *
	   * @param a the array whose string representation to return
	   * @return a string representation of <tt>a</tt>
	   * @since 1.5
	   */
	  public static String toString(double[] a) {
		  if (a == null)
			  return "null";
		  int iMax = a.length - 1;
		  if (iMax == -1)
			  return "[]";

		  StringBuilder b = new StringBuilder();
		  b.append('[');
		  for (int i = 0; ; i++) {
			  b.append(a[i]);
			  if (i == iMax)
				  return b.append(']').toString();
			  b.append(", ");
		  }
	  }

	  /**
	   * Returns a string representation of the contents of the specified array.
	   * If the array contains other arrays as elements, they are converted to
	   * strings by the {@link Object#toString} method inherited from
	   * <tt>Object</tt>, which describes their <i>identities</i> rather than
	   * their contents.
	   *
	   * <p>The value returned by this method is equal to the value that would
	   * be returned by <tt>Arrays.asList(a).toString()</tt>, unless <tt>a</tt>
	   * is <tt>null</tt>, in which case <tt>"null"</tt> is returned.
	   *
	   * @param a the array whose string representation to return
	   * @return a string representation of <tt>a</tt>
	   * @see #deepToString(Object[])
	   * @since 1.5
	   */
	  public static String toString(Object[] a) {
		  if (a == null)
			  return "null";
		  int iMax = a.length - 1;
		  if (iMax == -1)
			  return "[]";

		  StringBuilder b = new StringBuilder();
		  b.append('[');
		  for (int i = 0; ; i++) {
			  b.append(String.valueOf(a[i]));
			  if (i == iMax)
				  return b.append(']').toString();
			  b.append(", ");
		  }
	  }

	  /**
	   * Returns a string representation of the "deep contents" of the specified
	   * array.  If the array contains other arrays as elements, the string
	   * representation contains their contents and so on.  This method is
	   * designed for converting multidimensional arrays to strings.
	   *
	   * <p>The string representation consists of a list of the array's
	   * elements, enclosed in square brackets (<tt>"[]"</tt>).  Adjacent
	   * elements are separated by the characters <tt>", "</tt> (a comma
	   * followed by a space).  Elements are converted to strings as by
	   * <tt>String.valueOf(Object)</tt>, unless they are themselves
	   * arrays.
	   *
	   * <p>If an element <tt>e</tt> is an array of a primitive type, it is
	   * converted to a string as by invoking the appropriate overloading of
	   * <tt>Arrays.toString(e)</tt>.  If an element <tt>e</tt> is an array of a
	   * reference type, it is converted to a string as by invoking
	   * this method recursively.
	   *
	   * <p>To avoid infinite recursion, if the specified array contains itself
	   * as an element, or contains an indirect reference to itself through one
	   * or more levels of arrays, the self-reference is converted to the string
	   * <tt>"[...]"</tt>.  For example, an array containing only a reference
	   * to itself would be rendered as <tt>"[[...]]"</tt>.
	   *
	   * <p>This method returns <tt>"null"</tt> if the specified array
	   * is <tt>null</tt>.
	   *
	   * @param a the array whose string representation to return
	   * @return a string representation of <tt>a</tt>
	   * @see #toString(Object[])
	   * @since 1.5
	   */
	  public static String deepToString(Object[] a) {
		  if (a == null)
			  return "null";

		  int bufLen = 20 * a.length;
		  if (a.length != 0 && bufLen <= 0)
			  bufLen = Integer.MAX_VALUE;
		  StringBuilder buf = new StringBuilder(bufLen);
		  deepToString(a, buf, new HashSet());
		  return buf.toString();
	  }

	  private static void deepToString(Object[] a, StringBuilder buf,
			  Set<Object[]> dejaVu) {
		  if (a == null) {
			  buf.append("null");
			  return;
		  }
		  dejaVu.add(a);
		  buf.append('[');
		  for (int i = 0; i < a.length; i++) {
			  if (i != 0)
				  buf.append(", ");

			  Object element = a[i];
			  if (element == null) {
				  buf.append("null");
			  } else {
				  Class eClass = element.getClass();

				  if (eClass.isArray()) {
					  if (eClass == byte[].class)
						  buf.append(toString((byte[]) element));
					  else if (eClass == short[].class)
						  buf.append(toString((short[]) element));
					  else if (eClass == int[].class)
						  buf.append(toString((int[]) element));
					  else if (eClass == long[].class)
						  buf.append(toString((long[]) element));
					  else if (eClass == char[].class)
						  buf.append(toString((char[]) element));
					  else if (eClass == float[].class)
						  buf.append(toString((float[]) element));
					  else if (eClass == double[].class)
						  buf.append(toString((double[]) element));
					  else if (eClass == boolean[].class)
						  buf.append(toString((boolean[]) element));
					  else { // element is an array of object references
						  if (dejaVu.contains(element))
							  buf.append("[...]");
						  else
							  deepToString((Object[])element, buf, dejaVu);
					  }
				  } else {  // element is non-null and not an array
					  buf.append(element.toString());
				  }
			  }
		  }
		  buf.append(']');
		  dejaVu.remove(a);
	  }

}
