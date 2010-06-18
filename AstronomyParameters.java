package milkyway_server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.StringTokenizer;


public class AstronomyParameters {

	double background_weight;
	double[] background_parameters;
	double[] background_step;
	double[] background_min;
	double[] background_max;
	boolean[] optimize_background_parameter;

	double[] stream_weights;
	double[] stream_weight_steps;
	double[] stream_weight_min;
	double[] stream_weight_max;
	boolean[] optimize_stream_weight;

	double[][] stream_parameters;
	double[][] stream_steps;
	double[][] stream_mins;
	double[][] stream_maxs;
	boolean[][] optimize_stream_parameter;

	int convolve;
	int sgr_coordinates;
	int wedge;

	LinkedList<Integral> integrals = new LinkedList<Integral>();


	public int getNumberIntegrals() {
		return integrals.size();
	}

	public int getNumberStreams() {
		return stream_parameters.length;
	}

	public int getConvolve() {
		return convolve;
	}

	public LinkedList<Integral> getIntegrals() {
		return integrals;
	}

	public int getNumberParameters() {
		int count = 0;

		for (int i = 0; i < optimize_background_parameter.length; i++) {
			if (optimize_background_parameter[i]) count++;
		}

		for (int i = 0; i < optimize_stream_weight.length; i++) {
			if (optimize_stream_weight[i]) count++;

			for (int j = 0; j < optimize_stream_parameter[i].length; j++) {
				if (optimize_stream_parameter[i][j]) count++;
			}
		}

		return count;
	}

	public double[] getMinBound() {
		int count = 0;
		int numberParameters = getNumberParameters();
		double[] minBound = new double[numberParameters];

		for (int i = 0; i < optimize_background_parameter.length; i++) {
			if (optimize_background_parameter[i]) {
				minBound[count] = background_min[i];
				count++;
			}
		}

		for (int i = 0; i < optimize_stream_weight.length; i++) {
			if (optimize_stream_weight[i]) {
				minBound[count] = stream_weight_min[i];
				count++;
			}

			for (int j = 0; j < optimize_stream_parameter[i].length; j++) {
				if (optimize_stream_parameter[i][j]) {
					minBound[count] = stream_mins[i][j];
					count++;
				}
			}
		}

		return minBound;
	}

	public double[] getMaxBound() {
		int count = 0;
		int numberParameters = getNumberParameters();
		double[] maxBound = new double[numberParameters];

		for (int i = 0; i < optimize_background_parameter.length; i++) {
			if (optimize_background_parameter[i]) {
				maxBound[count] = background_max[i];
				count++;
			}
		}

		for (int i = 0; i < optimize_stream_weight.length; i++) {
			if (optimize_stream_weight[i]) {
				maxBound[count] = stream_weight_max[i];
				count++;
			}

			for (int j = 0; j < optimize_stream_parameter[i].length; j++) {
				if (optimize_stream_parameter[i][j]) {
					maxBound[count] = stream_maxs[i][j];
					count++;
				}
			}
		}

		return maxBound;
	}

	public static AstronomyParameters readFromFile(File file) {
		AstronomyParameters ap = null;

		if (!file.exists()) {
			System.err.println("Error: astronomy parameters file: " + file + " does not exist.");

			return null;
		}

		try {
			ap = new AstronomyParameters();

			BufferedReader in = new BufferedReader( new FileReader( file ) );

			in.readLine();

			ap.background_weight = parseDouble(in.readLine(), "background_weight");
			ap.background_parameters = parseDoubleArray(in.readLine(), "background_parameters");
			ap.background_step = parseDoubleArray(in.readLine(), "background_step");
			ap.background_min = parseDoubleArray(in.readLine(), "background_min");
			ap.background_max = parseDoubleArray(in.readLine(), "background_max");
			ap.optimize_background_parameter = parseBooleanArray(in.readLine(), "optimize_parameter");

			StringTokenizer st = new StringTokenizer(in.readLine(), " ,[]:");
			st.nextToken();
			int number_streams = Integer.parseInt(st.nextToken());

			ap.stream_weights = new double[number_streams];
			ap.stream_weight_steps = new double[number_streams];
			ap.stream_weight_min = new double[number_streams];
			ap.stream_weight_max = new double[number_streams];
			ap.optimize_stream_weight = new boolean[number_streams];

			ap.stream_parameters = new double[number_streams][];
			ap.stream_steps = new double[number_streams][];
			ap.stream_mins = new double[number_streams][];
			ap.stream_maxs = new double[number_streams][];
			ap.optimize_stream_parameter = new boolean[number_streams][];

			for (int i = 0; i < number_streams; i++) {
				ap.stream_weights[i] = parseDouble(in.readLine(), "stream_weight");
				ap.stream_weight_steps[i] = parseDouble(in.readLine(), "stream_weight_step");
				ap.stream_weight_min[i] = parseDouble(in.readLine(), "stream_weight_min");
				ap.stream_weight_max[i] = parseDouble(in.readLine(), "stream_weight_max");
				ap.optimize_stream_weight[i] = parseBoolean(in.readLine(), "optimize_weight");

				ap.stream_parameters[i] = parseDoubleArray(in.readLine(), "stream_parameters");
				ap.stream_steps[i] = parseDoubleArray(in.readLine(), "stream_step");
				ap.stream_mins[i] = parseDoubleArray(in.readLine(), "stream_min");
				ap.stream_maxs[i] = parseDoubleArray(in.readLine(), "stream_max");
				ap.optimize_stream_parameter[i] = parseBooleanArray(in.readLine(), "optimize_parameter");
			}

			ap.convolve = parseInteger(in.readLine(), "convolve");
			ap.sgr_coordinates = parseInteger(in.readLine(), "sgr_coordinates");
			ap.wedge = parseInteger(in.readLine(), "wedge");

			ap.integrals.add( new Integral(in) );

			int number_cuts = parseInteger(in.readLine(), "number_cuts");
			for (int i = 0; i < number_cuts; i++) {
				ap.integrals.add( new Integral(in) );
			}

		} catch (IOException e) {
			System.err.println("IOException reading astronomy parameters file: " + file);
			e.printStackTrace();

			ap = null;
		} catch (Exception e) {
			System.err.println("Exception reading astronomy parameters file: " + file);
			e.printStackTrace();

			ap = null;
		}

		return ap;
	}


	public static double[] parseDoubleArray(String line, String name) throws IOException {
		StringTokenizer st = new StringTokenizer(line, " :[],");

		if ( !st.nextToken().equals(name) ) throw new IOException("line: " + line + " did not match name: " + name + " in parse double array");

		double[] result = new double[ Integer.parseInt(st.nextToken()) ];
		for (int i = 0; i < result.length; i++) result[i] = Double.parseDouble( st.nextToken() );

		return result;
	}

	public static boolean[] parseBooleanArray(String line, String name) throws IOException {
		StringTokenizer st = new StringTokenizer(line, " :[],");

		if ( !st.nextToken().equals(name) ) throw new IOException("line: " + line + " did not match name: " + name + " in parse boolean array");

		boolean[] result = new boolean[ Integer.parseInt(st.nextToken()) ];
		for (int i = 0; i < result.length; i++) result[i] = Integer.parseInt( st.nextToken() ) == 1;

		return result;
	}


	public static double parseDouble(String line, String name) throws IOException {
		StringTokenizer st = new StringTokenizer(line, " :[],");

		if ( !st.nextToken().equals(name) ) throw new IOException("line: " + line + " did not match name: " + name + " in parse double");

		return Double.parseDouble( st.nextToken() );
	}

	public static boolean parseBoolean(String line, String name) throws IOException {
		StringTokenizer st = new StringTokenizer(line, " :[],");

		if ( !st.nextToken().equals(name) ) throw new IOException("line: " + line + " did not match name: " + name + " in parse boolean");

		String value = st.nextToken();

		if (value.equals("1")) return true;
		else if (value.equals("0")) return false;
		else if (value.equals("true")) return true;
		else if (value.equals("false")) return false;
		else {
			throw new IOException(" could not parse boolean from line: " + line);
		}
	}

	public static int parseInteger(String line, String name) throws IOException {
		StringTokenizer st = new StringTokenizer(line, " :[],");

		if ( !st.nextToken().equals(name) ) throw new IOException("line: " + line + " did not match name: " + name + " in parse integer");

		return Integer.parseInt( st.nextToken() );
	}

}
