package milkyway_server;

import java.io.BufferedReader;
import java.io.IOException;

import java.util.StringTokenizer;

public class Integral {

	double r_min, r_max;
	int r_steps;

	double mu_min, mu_max;
	int mu_steps;

	double nu_min, nu_max;
	int nu_steps;

	public int getRSteps() { return r_steps; }
	public int getMuSteps() { return mu_steps; }
	public int getNuSteps() { return nu_steps; }

	public Integral(BufferedReader in) throws IOException {
		String line = in.readLine();

		StringTokenizer st = new StringTokenizer(line, " [],:");
		if ( !(st.nextToken().equals("r") && st.nextToken().equals("min") && st.nextToken().equals("max") && st.nextToken().equals("steps")) ) {
			throw new IOException("Could not read r parameters from line: " + line);
		}
		r_min = Double.parseDouble(st.nextToken());
		r_max = Double.parseDouble(st.nextToken());
		r_steps = Integer.parseInt(st.nextToken());

		line = in.readLine();
		st = new StringTokenizer(line, " [],:");
		if ( !(st.nextToken().equals("mu") && st.nextToken().equals("min") && st.nextToken().equals("max") && st.nextToken().equals("steps")) ) {
			throw new IOException("Could not read mu parameters from line: " + line);
		}
		mu_min = Double.parseDouble(st.nextToken());
		mu_max = Double.parseDouble(st.nextToken());
		mu_steps = Integer.parseInt(st.nextToken());

		line = in.readLine();
		st = new StringTokenizer(line, " [],:");
		if ( !(st.nextToken().equals("nu") && st.nextToken().equals("min") && st.nextToken().equals("max") && st.nextToken().equals("steps")) ) {
			throw new IOException("Could not read nu parameters from line: " + line);
		}
		nu_min = Double.parseDouble(st.nextToken());
		nu_max = Double.parseDouble(st.nextToken());
		nu_steps = Integer.parseInt(st.nextToken());

	}
}
