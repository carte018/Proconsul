package edu.duke.oit.idms.proconsul.util;

import edu.duke.oit.idms.proconsul.Status;

public class ProconsulContainer {

	private ProconsulSession session;
	private Status status;
	private DockerContainer container;
	
	// Constructors
	public ProconsulContainer() {
		// Blind create
		session = new ProconsulSession();
		status = Status.STARTING;
		container = new DockerContainer();
	}

	public ProconsulSession getSession() {
		return session;
	}

	public void setSession(ProconsulSession session) {
		this.session = session;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public DockerContainer getContainer() {
		return container;
	}

	public void setContainer(DockerContainer container) {
		this.container = container;
	}
	
}
