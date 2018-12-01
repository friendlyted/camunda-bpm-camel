package org.camunda.bpm.camel.itest;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;

import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_PROCESS_INSTANCE_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Single test class for single Karaf distribution.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class KarafDistributionTest {

  private final String smokeTestProcess = "smokeTestProcess";
  private final String receiveFromCamelProcess = "receiveFromCamelProcess";

  @Inject
  private BundleContext bundleContext;

  @Inject
  private RuntimeService runtimeService;

  @Inject
  private HistoryService historyService;

  @Inject
  private TaskService taskService;

  @Inject
  @Filter("(camel.context.name=smokeTestContext)")
  private CamelContext smokeTestContext;

  @Inject
  @Filter("(camel.context.name=receiveFromCamelContext)")
  private CamelContext receiveFromCamelContext;

  @Inject
  @Filter("(camel.context.name=startProcessFromRouteContext)")
  private CamelContext startProcessFromRouteContext;

  @Inject
  @Filter("(camel.context.name=sendToCamelContext)")
  private CamelContext sendToCamelContext;

  private MockEndpoint smokeTestRouteResultMock;
  private MockEndpoint receiveFromCamelRouteResultMock;
  private MockEndpoint sendToCamelResultMock;

  @Before
  public void resolveEndpoints() {
    smokeTestRouteResultMock = MockEndpoint.resolve(smokeTestContext, "mock:smokeTestRouteResultMock");
    receiveFromCamelRouteResultMock = MockEndpoint.resolve(receiveFromCamelContext, "mock:receiveFromCamelRouteResultMock");
    sendToCamelResultMock = MockEndpoint.resolve(sendToCamelContext, "mock:sendToCamelResultMock");
  }

  @Test
  public void testSmokeProcess() {
    assertNotNull(bundleContext);
    assertNotNull(runtimeService);
    assertNotNull(taskService);
    assertNotNull(smokeTestContext);

    runtimeService.startProcessInstanceByKey(smokeTestProcess);
    final Task task = taskService.createTaskQuery().processDefinitionKey(smokeTestProcess).singleResult();
    assertNotNull(task);
    assertEquals("Smoke Task", task.getName());
    assertEquals(
      runtimeService.createProcessInstanceQuery().processDefinitionKey(smokeTestProcess).count(),
      1L
    );
    taskService.complete(task.getId());
    assertEquals(
      runtimeService.createProcessInstanceQuery().processDefinitionKey(smokeTestProcess).count(),
      0L
    );
  }

  @Test
  public void testSmokeRoute() throws InterruptedException {
    assertNotNull(smokeTestRouteResultMock);
    smokeTestRouteResultMock.expectedMessageCount(1);
    smokeTestRouteResultMock.assertIsSatisfied();
  }

  @Test
  public void testReceiveFromCamel() {
    final String processInstanceId = runtimeService
      .startProcessInstanceByKey(receiveFromCamelProcess)
      .getId();

    // Verify that a process instance has executed and there is one instance executing now
    assertEquals(
      runtimeService
        .createExecutionQuery()
        .processInstanceId(processInstanceId)
        .activityId("waitForCamel")
        .count(),
      1L
    );

    /*
     * We need the process instance ID to be able to send the message to it
     *
     * FIXME: we need to fix this with the process execution id or even better with the Activity Instance Model
     * http://camundabpm.blogspot.de/2013/06/introducing-activity-instance-model-to.html
     */
    final ProducerTemplate tpl = receiveFromCamelContext.createProducerTemplate();
    final String direct = "direct:receiveFromCamelStartDirect";
    final Object body = null;

    tpl.sendBodyAndProperty(
      direct,
      body,
      EXCHANGE_HEADER_PROCESS_INSTANCE_ID,
      processInstanceId
    );

    // Assert that the process instance is finished
    assertEquals(
      runtimeService
        .createProcessInstanceQuery()
        .processDefinitionKey("receiveFromCamelProcess")
        .count(),
      0L
    );
  }

  @Test
  public void testStartProcessFromRoute() {
    final ProducerTemplate tpl = startProcessFromRouteContext.createProducerTemplate();
    final String direct = "direct:startProcessFromRouteStartDirect";
    final Object body = "BoDy";

    assertEquals(
      historyService
        .createHistoricProcessInstanceQuery()
        .processDefinitionKey("startProcessFromRoute")
        .count(),
      0L
    );

    tpl.sendBody(direct, body);

    assertEquals(
      historyService
        .createHistoricProcessInstanceQuery()
        .processDefinitionKey("startProcessFromRoute")
        .count(),
      1L
    );

    assertEquals(
      historyService
        .createHistoricVariableInstanceQuery()
        .processDefinitionKey("startProcessFromRoute")
        .variableName("var1")
        .list().get(0).getValue(),
      body
    );

  }

  @Test
  public void testSendToCamelRoute() {
    runtimeService
      .startProcessInstanceByKey("sendToCamelProcess");

//    sendToCamelResultMock.assertExchangeReceived(1);
  }


}
