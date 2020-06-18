package edu.duke.oit.idms.proconsul.util;

import java.util.ArrayList;
import java.util.HashMap;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
//import com.sun.istack.internal.logging.Logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.duke.oit.idms.proconsul.cfg.PCConfig;
import edu.duke.oit.idms.proconsul.exceptions.ContainerInitException;

public class DockerContainer {

	// Wrapper class for Docker library
	
	// Internal values
	private DockerClient docker;
	private CreateContainerCmd command;
	private CreateContainerResponse container;
	
	private String imageName;
	private HashMap<Integer,Integer> portMappings;
	private HashMap<String,String> volumeMappings;
	private ArrayList<String> environment;
	private String cpuset;
	private boolean privileged;
	private String cmd;
	
	private String dockerHost;
	
	private static final Logger LOG = LoggerFactory.getLogger(DockerContainer.class);
	
	// Constructors
	
	public DockerContainer() {
		imageName = null;
		portMappings = new HashMap<Integer,Integer>();
		volumeMappings = new HashMap<String,String>();
		//
		// We must now distinguish two cases for mapping the /var/spool/docker volume in the target container
		// If the deployment is host-oriented (not dockerized) we mount the host's /var/spool/docker directory on 
		// /var/spool/docker in the container.
		// If the deployment is dockerized, we mount a data volume (proconsul-spool-data) on the same directory.
		// This is to allow for handshaking between Proconsul and the rdpstack/vncstack container.
		// Dockerization is detected based on a configuration option.
		
		PCConfig config = PCConfig.getInstance();
		String dtype = config.getProperty("deployment.type", false);
		if (dtype != null && dtype.equalsIgnoreCase("docker")) {
			volumeMappings.put("/var/spool/docker","proconsul-spool-data");
		} else {
			volumeMappings.put("/var/spool/docker", "/var/spool/docker");
		}
		environment = new ArrayList<String>();
		cpuset = null;
		privileged = true;
		command = null;
	}
	
	public DockerContainer(ProconsulSession sess) {
		PCConfig config = PCConfig.getInstance();
		imageName = config.getProperty("docker.image", true);
		cpuset = config.getProperty("docker.cpuset", true);
		dockerHost = config.getProperty("docker.host", true);
		privileged = true; // always run privileged
		// Compute port mappings from the session
		portMappings = new HashMap<Integer,Integer>();
		portMappings.put(5901+sess.getNovncPort(),5901+sess.getNovncPort());
		volumeMappings = new HashMap<String,String>();
		volumeMappings.put(config.getProperty("mysql.socket", true), config.getProperty("mysql.socket", true));
		String dtype = config.getProperty("deployment.type",  false);
		if (dtype != null && dtype.equalsIgnoreCase("docker")) {
			volumeMappings.put("/var/spool/docker","proconsul-spool-data");
		} else {
			volumeMappings.put("/var/spool/docker", "/var/spool/docker");
		}
		environment = new ArrayList<String>();
		environment.add("VNCPASSWORD="+sess.getVncPassword());
		environment.add("WINUSER="+sess.getTargetUser().getsAMAccountName());
		environment.add("DOMAIN="+config.getProperty("ldap.domain", true));
		environment.add("WINPASS="+sess.getTargetUser().getAdPassword());
		environment.add("RHOSTNAME="+sess.getFqdn());
		environment.add("PORTNUM="+String.valueOf(sess.getNovncPort() + 5900));
		environment.add("DISPLAY="+String.valueOf(sess.getNovncPort()));
		environment.add("EXT="+String.valueOf(sess.getNovncPort() + 5901));
		environment.add("HOME="+config.getProperty("novnc.home", true));
		environment.add("HOSTNAME="+config.getProperty("novnc.hostname", true));
		environment.add("MYSQLUSER="+config.getProperty("pcdb.user", true));
		environment.add("MYSQLPW="+config.getProperty("pcdb.password", true));
		// if there is a gateway, add it to the environment
		if (sess.getGatewayfqdn() != null) {
			environment.add("GATEWAY="+sess.getGatewayfqdn());
		}
		command = null;
	}

	public DockerClient getDocker() {
		return docker;
	}

	public void setDocker(DockerClient docker) {
		this.docker = docker;
	}

	public CreateContainerCmd getCommand() {
		return command;
	}

	public void setCommand(CreateContainerCmd command) {
		this.command = command;
	}

	public CreateContainerResponse getContainer() {
		return container;
	}

	public void setContainer(CreateContainerResponse container) {
		this.container = container;
	}

	public String getImageName() {
		return imageName;
	}

	public void setImageName(String imageName) {
		this.imageName = imageName;
	}

	public HashMap<Integer, Integer> getPortMappings() {
		return portMappings;
	}

	public void setPortMappings(HashMap<Integer, Integer> portMappings) {
		this.portMappings = portMappings;
	}

	public HashMap<String, String> getVolumeMappings() {
		return volumeMappings;
	}

	public void setVolumeMappings(HashMap<String, String> volumeMappings) {
		this.volumeMappings = volumeMappings;
	}

	public ArrayList<String> getEnvironment() {
		return environment;
	}

	public void setEnvironment(ArrayList<String> environment) {
		this.environment = environment;
	}

	public String getCpuset() {
		return cpuset;
	}

	public void setCpuset(String cpuset) {
		this.cpuset = cpuset;
	}

	public boolean isPrivileged() {
		return privileged;
	}

	public void setPrivileged(boolean privileged) {
		this.privileged = privileged;
	}

	public String getCmd() {
		return cmd;
	}

	public void setCmd(String cmd) {
		this.cmd = cmd;
	}
	
	// Utility method for invoking start on the container
	
	public void start() {
		start("default");
	}
	
	public void start(String size) {
		PCConfig config = PCConfig.getInstance();
				
		
		if (imageName == null || imageName.equalsIgnoreCase("")) {
			throw new ContainerInitException("Imagename is required for initialization");
		}
		if (cpuset == null || cpuset.equalsIgnoreCase("")) {
			throw new ContainerInitException("cpuset is required for initialization");
		}
		
		ProconsulUtils.debug(LOG,"Calling docker api");
		docker = DockerClientBuilder.getInstance("http://" + dockerHost + ":2375/v1.19").build();
		// docker = DockerClientBuilder.getInstance("http://localhost:2375").build();
		ProconsulUtils.debug(LOG,"Docker connection established");
		if (docker == null) {
			throw new ContainerInitException("Connection to docker executive failed");
		}
		ArrayList<PortBinding> portBindings = new ArrayList<PortBinding>();
		ArrayList<ExposedPort> exposedPorts = new ArrayList<ExposedPort>();
		if (portMappings != null && ! portMappings.isEmpty()) {
			
			for (int hostPort : portMappings.keySet()) {
				int containerPort = portMappings.get(hostPort);
				ExposedPort ep = new ExposedPort(containerPort, InternetProtocol.TCP);
				PortBinding hostBind = new PortBinding(new Ports.Binding(hostPort),ep);
				exposedPorts.add(ep);
				portBindings.add(hostBind);
				
			}
		//	command.withPortBindings(portBindings.toArray(new PortBinding[portBindings.size()]));
		//	System.out.println("Exposed port bindings");
		}
		ProconsulUtils.debug(LOG,"Creating new container");
		if (size != null && size.equals("large")) 
			command = docker.createContainerCmd(config.getProperty("docker.image.large", true)).withPrivileged(privileged).withCpuset(cpuset).withNetworkMode("host").withPortBindings(portBindings.toArray(new PortBinding[portBindings.size()]));
		else if (size != null && size.equals("vnclarge"))
			command = docker.createContainerCmd(config.getProperty("docker.image.vnclarge", true)).withPrivileged(privileged).withCpuset(cpuset).withNetworkMode("host").withPortBindings(portBindings.toArray(new PortBinding[portBindings.size()]));
		else if (size != null & size.equals("vncdefault"))
			command = docker.createContainerCmd(config.getProperty("docker.image.vncdefault", true)).withPrivileged(privileged).withCpuset(cpuset).withNetworkMode("host").withPortBindings(portBindings.toArray(new PortBinding[portBindings.size()]));
		else	
			command = docker.createContainerCmd(imageName).withPrivileged(privileged).withCpuset(cpuset).withNetworkMode("host").withPortBindings(portBindings.toArray(new PortBinding[portBindings.size()]));
		ProconsulUtils.log(LOG,"New container created");
		if (command == null) {
			ProconsulUtils.error(LOG, "Failed creating new container");
			throw new ContainerInitException("Creation of new container failed");
		}
		if (volumeMappings != null && ! volumeMappings.isEmpty()) {
			Bind[] vbinds = new Bind[volumeMappings.size()];
			Volume[] vols = new Volume[volumeMappings.size()];
			
			int count = 0;
			for (String v : volumeMappings.keySet()) {
				vols[count] = new Volume(v);
				vbinds[count] = new Bind(volumeMappings.get(v),vols[count]);
				count ++;
			}
			command.withVolumes(vols).withBinds(vbinds);
			ProconsulUtils.debug(LOG,"Set " + count + " volume binds");
		}
		if (environment != null && ! environment.isEmpty()) {
			command.withEnv(environment.toArray(new String[1]));
			ProconsulUtils.debug(LOG,"Set environment values");
		}
		if (cmd != null && ! cmd.equalsIgnoreCase("")) {
			String[] cmdparts = cmd.split(" ");
			command.withCmd(cmdparts);
			ProconsulUtils.debug(LOG,"Set cmd to " + cmd);
		}
		container = command.exec();
		ProconsulUtils.debug(LOG,"Execed");
		if (container == null) {
			ProconsulUtils.error(LOG,"Failed to create new container with specified arguments");
			throw new ContainerInitException("Failed to create container with arguments");
		}
		docker.startContainerCmd(container.getId()).exec();
		
	}
}