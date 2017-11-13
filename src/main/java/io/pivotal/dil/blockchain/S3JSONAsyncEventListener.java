package io.pivotal.dil.blockchain;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import org.apache.geode.cache.Declarable;
import org.apache.geode.cache.Operation;
import org.apache.geode.cache.asyncqueue.AsyncEvent;
import org.apache.geode.cache.asyncqueue.AsyncEventListener;
import org.apache.geode.pdx.PdxInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

import io.pivotal.dil.blockchain.entity.BlockchainTxn;

public class S3JSONAsyncEventListener implements AsyncEventListener, Declarable {

	private AmazonS3 client;
	private String bucketName;

	private static final Logger LOG = LoggerFactory.getLogger(S3JSONAsyncEventListener.class);

	public S3JSONAsyncEventListener() {
		super();
	}

	/*
	 * Properties to provide:
	 * 
	 * awsRegion, s3Bucket, s3AccessKeyID, s3SecretAccessKey
	 * 
	 */
	@Override
	public void init(Properties props) {
		String awsRegion = props.getProperty("awsRegion", Regions.US_WEST_2.name());
		bucketName = props.getProperty("s3Bucket");
		LOG.info("init: awsRegion={}, s3Bucket={}", awsRegion, bucketName);
		Regions awsr = Regions.fromName(awsRegion);
		// Get credentials
		AWSCredentials credentials = new BasicAWSCredentials(props.getProperty("s3AccessKeyID"),
				props.getProperty("s3SecretAccessKey"));
		client = new AmazonS3Client(credentials);
		client.setRegion(Region.getRegion(awsr));
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean processEvents(List<AsyncEvent> events) {
		LOG.info("processEvents: events.size={}", events.size());
		boolean rv = true;
		Operation op;
		Object value = null;
		String  valueAsJson = "{}";
		try {
			for (AsyncEvent evt : events) {
				LOG.debug("processEvents: evt={}", evt);
				op = evt.getOperation();
				String regionName = evt.getRegion().getName();

				Object ok = evt.getKey();
				if (ok.getClass() != String.class) {
					throw new IllegalArgumentException("Only String keys are supported: " + ok);
				}

				String key = ok.toString();
				String path = regionName + "/" + key;

				if ((op.isCreate() || op.isUpdate()) && !op.isLoad()) {
					value = evt.getDeserializedValue(); // TYPE: org.apache.geode.pdx.internal.PdxInstanceImpl
					valueAsJson = ((BlockchainTxn) ((PdxInstance) value).getObject()).toJSON();
					int len = valueAsJson.length();
					LOG.info("processEvents: put: key={}, len={}, regionName={}", key, len, regionName);
					ObjectMetadata meta = new ObjectMetadata();
					meta.setContentLength(len);
					meta.setContentType("application/json");
					InputStream bis = new ByteArrayInputStream(valueAsJson.getBytes(StandardCharsets.UTF_8.name()));
					client.putObject(bucketName, path, bis, meta);
				} else if (op.isDestroy() && !(op.isEviction() || op.isExpiration())) {
					LOG.info("processEvents: delete: key={}, regionName={}", key, regionName);
					client.deleteObject(bucketName, path);
				} else {
					LOG.info("processEvents: NOT a create, update, or destroy: evt={}", evt);
				}
			}
		} catch (Exception x) {
			rv = false;
			throw new RuntimeException(x);
		}
		return rv;
	}

	@Override
	public void close() {
	}

}
