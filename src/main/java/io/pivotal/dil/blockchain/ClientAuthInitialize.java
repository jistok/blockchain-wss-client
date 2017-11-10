package io.pivotal.dil.blockchain;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gemstone.gemfire.LogWriter;
import com.gemstone.gemfire.distributed.DistributedMember;
import com.gemstone.gemfire.security.AuthInitialize;
import com.gemstone.gemfire.security.AuthenticationFailedException;

public class ClientAuthInitialize implements AuthInitialize {

	public static final String USER_NAME = "security-username";
	public static final String PASSWORD = "security-password";

	private static final Logger LOG = LogManager.getLogger(ClientAuthInitialize.class);

	public static AuthInitialize create() {
		LOG.info("ClientAuthInitialize.create");
		return new ClientAuthInitialize();
	}

	public ClientAuthInitialize() {
		LOG.info("ClientAuthInitialize.constructor");
	}

	@Override
	public void init(LogWriter systemLogger, LogWriter securityLogger) throws AuthenticationFailedException {
		LOG.info("ClientAuthInitialize.init");
	}

	@Override
	public Properties getCredentials(Properties securityProps, DistributedMember server, boolean isPeer)
			throws AuthenticationFailedException {
		LOG.info("ClientAuthInitialize.getCredentials");
		Properties newProps = new Properties();
		String userName = securityProps.getProperty(USER_NAME);
		if (userName == null) {
			throw new AuthenticationFailedException(
					"ClientAuthInitialize: user name property [" + USER_NAME + "] not set.");
		}
		String passwd = securityProps.getProperty(PASSWORD);
		if (passwd == null) {
			passwd = "";
		}
		newProps.setProperty(USER_NAME, userName);
		newProps.setProperty(PASSWORD, passwd);
		return newProps;
	}

	@Override
	public void close() {
	}

}
