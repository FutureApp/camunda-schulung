package com.camunda.training;

import org.assertj.core.api.Assert;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests;
import org.camunda.bpm.extension.process_test_coverage.junit5.ProcessEngineCoverageExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.complete;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.execute;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.externalTask;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.findId;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.jobQuery;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.runtimeService;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.task;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.withVariables;

@ExtendWith(ProcessEngineCoverageExtension.class)
public class ProcessJUnitTest {

    @Test
    @Deployment(resources = "Process_TwitterQA.bpmn")
    public void happyPath() {
        // Create a HashMap to put in variables for the process instance
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        variables.put("content", "Bob True");
        // Start process with Java API and variables
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TwitterQAProcess", variables);

        BpmnAwareTests.assertThat(processInstance).isWaitingAt("Activity_Message_Checking");
        BpmnAwareTests.assertThat(processInstance).task().hasCandidateGroup("management");

        complete(task(), withVariables("approved", true));

        List<Job> jobList = jobQuery()
                .processInstanceId(processInstance.getId())
                .list();
        Assertions.assertEquals(jobList.size(), 1);
        Job job = jobList.get(0);
        execute(job);

        BpmnAwareTests.assertThat(processInstance).isEnded();
    }


    @Test
    @Deployment(resources = "Process_TwitterQA.bpmn")
    public void testMsgRejected() {
        var varMap = new HashMap<String, Object>();
        varMap.put("approved", false);
        varMap.put("content", "Bob");

        ProcessInstance processInstance = runtimeService()
                .createProcessInstanceByKey("TwitterQAProcess")
                .setVariables(varMap)
                .startAfterActivity(findId("Nachricht prüfen"))
                .execute();

        BpmnAwareTests.assertThat(processInstance)
                .isWaitingAt(findId("Send notification"))
                .externalTask()
                .hasTopicName("notifications");
        BpmnAwareTests.complete(externalTask());


        BpmnAwareTests.assertThat(processInstance).isEnded().hasPassed(findId("Author informieren"));

    }

    @Test
    @Deployment(resources = "Process_TwitterQA.bpmn")
    public void testMSGStart() {
        ProcessInstance processInstance = runtimeService()
                .createMessageCorrelation("superuserTweet")
                .setVariable("content", "My Exercise 11 Tweet (HelloWorld)- " + System.currentTimeMillis())
                .correlateWithResult()
                .getProcessInstance();


        // get the job
        List<Job> jobList = jobQuery()
                .processInstanceId(processInstance.getId())
                .list();

        // execute the job
        assert jobList.size() == 1;
        Job job = jobList.get(0);
        execute(job);

        BpmnAwareTests.assertThat(processInstance).isEnded();
        BpmnAwareTests.assertThat(processInstance).isStarted();
    }
    @Test
    @Deployment(resources="Process_TwitterQA.bpmn")
    public void testTweetWithdrawn() {
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("content", "Test tweetWithdrawn message");
        ProcessInstance processInstance = runtimeService()
                .startProcessInstanceByKey("TwitterQAProcess", varMap);
        BpmnAwareTests.assertThat(processInstance).isStarted().isWaitingAt(findId("Nachricht prüfen"));
        runtimeService()
                .createMessageCorrelation("tweetWithdrawn")
                .processInstanceVariableEquals("content", "Test tweetWithdrawn message")
                .correlateWithResult();
        BpmnAwareTests.assertThat(processInstance).isEnded();
    }

}
