package io.pivotal.dil.blockchain;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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

	private List<String> jsonEventList;
	private static final int S3_BATCH_SIZE = 1000;
	private static final BlockingQueue<String> EVENT_QUEUE = new LinkedBlockingQueue<>(S3_BATCH_SIZE);
	private static final long QUEUE_OFFER_TIMEOUT_MS = 5L;
	
	// File names for S3 encode the date of the last transaction in the batch
	private final DateFormat DATE_FORMAT_FOR_S3_FILENAMES = new SimpleDateFormat("yyyyMMdd-HHmmss");

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
		jsonEventList = new ArrayList<>(S3_BATCH_SIZE);
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
		try {
			for (AsyncEvent evt : events) {
				LOG.debug("processEvents: evt={}", evt);
				Operation op = evt.getOperation();
				String regionName = evt.getRegion().getName();

				Object ok = evt.getKey();
				if (ok.getClass() != String.class) {
					throw new IllegalArgumentException("Only String keys are supported: " + ok);
				}
				String key = ok.toString();

				if ((op.isCreate() || op.isUpdate()) && !op.isLoad()) { // TODO: Still fuzzy on these qualifications
					PdxInstance value = (PdxInstance) evt.getDeserializedValue();
					BlockchainTxn txn = (BlockchainTxn) value.getObject();
					String jsonStr = txn.toJSON();
					synchronized (EVENT_QUEUE) {
						if (!EVENT_QUEUE.offer(jsonStr, QUEUE_OFFER_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
							jsonEventList.clear();
							EVENT_QUEUE.drainTo(jsonEventList);
							String entireBatchOfJson = String.join("\n",
									jsonEventList.toArray(new String[jsonEventList.size()]));
							int len = entireBatchOfJson.length();
							LOG.info("processEvents: put: key={}, len={}, regionName={}", key, len, regionName);
							ObjectMetadata meta = new ObjectMetadata();
							meta.setContentLength(len);
							meta.setContentType(/* "application/json" */ "text/plain");
							String path = regionName + "/" + DATE_FORMAT_FOR_S3_FILENAMES.format(txn.getTimeAsDate());
							InputStream bis = new ByteArrayInputStream(
									entireBatchOfJson.getBytes(StandardCharsets.UTF_8.name()));
							client.putObject(bucketName, path, bis, meta);
							EVENT_QUEUE.put(jsonStr);
						}
					}
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
