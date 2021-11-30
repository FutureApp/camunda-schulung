package com.camunda.training;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.extension.junit5.test.ProcessEngineExtension;
import org.camunda.bpm.extension.process_test_coverage.junit5.ProcessEngineCoverageExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(ProcessEngineCoverageExtension.class)
public class ProcessJUnitTest {

    @Test
    @Deployment(resources = "Process_TwitterQA.bpmn")
    public void happyPath() {
        // Create a HashMap to put in variables for the process instance
        Map<String, Object> variables = new HashMap<String, Object>();
        // Start process with Java API and variables
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TwitterQAProcess", variables);
        // Make assertions on the process instance
        String msgPruefenTaskId = findId("Nachricht prüfen"); // Sucht die ID via den Namen

        List<Task> taskList = taskService()
                .createTaskQuery()
                .taskCandidateGroup("management")
                .processInstanceId(processInstance.getId())
                .list();

        assertThat(taskList).isNotNull();
        assertThat(taskList).hasSize(1);
        Task task = taskList.get(0);

        Map<String, Object> approvedMap = new HashMap<>();
        approvedMap.put("approved", true);
        taskService().complete(task.getId(), approvedMap); // Führt den Task zu Ende

        assertThat(processInstance).isEnded();
    }
}
