package com.xpanse.los;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpanse.los.model.Response;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.SfnException;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StartExecutionResponse;

/** Java Class App */
public class App {

	/** Lamdba Function to be used as service */
	public void handleRequest(final Map<String, Object> input, final Context context) {
		context.getLogger().log("income-stepfunction-invocation-processor | Start : " + context.getAwsRequestId(),
				LogLevel.DEBUG);

		/** Start Step Function execution */
		final SfnClient sfnClient = SfnClient.builder().region(Region.of(System.getenv("STEP_FUNCTION_REGION")))
				.build();

		try {

			List<Map<String, Object>> rec = (List<Map<String, Object>>) input.get("Records");
			context.getLogger().log("Message list size: " + rec.size(), LogLevel.DEBUG);

			final ObjectMapper objMp = new ObjectMapper();
			for (int i = 0; i < rec.size(); i++) {
				Response requestPayload = new Response();
				requestPayload.setAction("start");
				requestPayload.setPayload(rec.get(i).get("body").toString());

				context.getLogger().log("Message : " + rec.get(i).get("body"), LogLevel.DEBUG);
				startWorkflow(sfnClient, System.getenv("STEP_FUNCTION_ARN"), objMp.writeValueAsString(requestPayload));
			}

		} catch (SfnException e) {
			e.printStackTrace();
			context.getLogger().log(e.awsErrorDetails().errorMessage(), LogLevel.ERROR);
		} catch (Exception e) {
			e.printStackTrace();
			context.getLogger().log(e.getMessage(), LogLevel.ERROR);
		}

		context.getLogger().log("income-stepfunction-invocation-processor | End : "+ context.getAwsRequestId(),LogLevel.DEBUG);

	}

	private String startWorkflow(SfnClient sfnClient, String stateMachineArn, String jsonEx) throws SfnException {
		UUID uuid = UUID.randomUUID();
		String uuidValue = uuid.toString();
		StartExecutionRequest executionRequest = StartExecutionRequest.builder().input(jsonEx)
				.stateMachineArn(stateMachineArn).name(uuidValue).build();

		StartExecutionResponse response = sfnClient.startExecution(executionRequest);
		return response.executionArn();

	}

}
