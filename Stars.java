package milkyway_server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.util.StringTokenizer;

public class Stars {

	private double[][] star_points;

	public int getNumberStars() {
		return star_points.length;
	}

	public Stars() {
	}

	public static Stars readFromFile(File file) {
		if (!file.exists()) {
			System.err.println("Could not read star file: " + file + " -- it does not exist.");
			return null;
		}

		try {
			BufferedReader in = new BufferedReader( new FileReader(file) );

			Stars sp = new Stars();

			sp.star_points = new double[ Integer.parseInt( in.readLine() ) ][3];

			for (int i = 0; i < sp.star_points.length; i++) {
				StringTokenizer st = new StringTokenizer(in.readLine(), " ");

				sp.star_points[i][0] = Double.parseDouble( st.nextToken() );
				sp.star_points[i][1] = Double.parseDouble( st.nextToken() );
				sp.star_points[i][2] = Double.parseDouble( st.nextToken() );
			}
			return sp;
		} catch (Exception e) {
			System.err.println("Exception while reading star file: " + file + ", " + e);
			e.printStackTrace();

			return null;
		}

	}
}
