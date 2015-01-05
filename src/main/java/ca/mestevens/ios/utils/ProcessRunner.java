package ca.mestevens.ios.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

public class ProcessRunner {

	public Log log;
	public boolean expectZeros;

	public ProcessRunner(Log log) {
		this.log = log;
		this.expectZeros = true;
	}
	
	public ProcessRunner(Log log, boolean expectZeros) {
		this.log = log;
		this.expectZeros = expectZeros;
	}

	public int runProcess(String workingDirectory, String... strings) throws MojoFailureException {
		try {
			ProcessBuilder processBuilder = new ProcessBuilder(strings);
			if (workingDirectory != null) {
				processBuilder.directory(new File(workingDirectory));
			}
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();
			BufferedReader input = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			String line = null;
			while ((line = input.readLine()) != null) {
				log.info(line);
			}
			int executionValue = process.waitFor();
			if (expectZeros && executionValue != 0) {
				throw new MojoFailureException(String.format("Process %s returned %d.", processBuilder.command(), executionValue));
			}
			return executionValue;
		} catch (IOException e) {
			log.error(e.getMessage());
			throw new MojoFailureException(e.getMessage());
		} catch (InterruptedException e) {
			log.error(e.getMessage());
			throw new MojoFailureException(e.getMessage());
		}
	}

}
