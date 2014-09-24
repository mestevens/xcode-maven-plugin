package ca.mestevens.ios.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.maven.plugin.MojoFailureException;

import org.apache.maven.plugin.logging.Log;

public class ProcessRunner {
	
	private Log log;
	
	public ProcessRunner(Log log) {
		this.log = log;
	}
	
	public int runProcess(String... strings) throws MojoFailureException {
		try {
		ProcessBuilder processBuilder = new ProcessBuilder(strings);
		processBuilder.redirectErrorStream(true);
		Process process = processBuilder.start();
		BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line = null;
		while ((line = input.readLine()) != null) {
			log.info(line);
		}
		return process.waitFor();
	} catch (IOException e) {
		log.error(e.getMessage());
		throw new MojoFailureException(e.getMessage());
	} catch (InterruptedException e) {
		log.error(e.getMessage());
		throw new MojoFailureException(e.getMessage());
	}
	}
	
	

}
