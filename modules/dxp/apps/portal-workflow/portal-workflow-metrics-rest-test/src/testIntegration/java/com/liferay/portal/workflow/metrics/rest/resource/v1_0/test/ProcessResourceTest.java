/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Liferay Enterprise
 * Subscription License ("License"). You may not use this file except in
 * compliance with the License. You can obtain a copy of the License by
 * contacting Liferay, Inc. See the License for the specific language governing
 * permissions and limitations under the License, including but not limited to
 * distribution rights of the Software.
 *
 *
 *
 */

package com.liferay.portal.workflow.metrics.rest.resource.v1_0.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.function.UnsafeBiConsumer;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.odata.entity.EntityField;
import com.liferay.portal.search.document.Document;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.workflow.metrics.rest.client.dto.v1_0.Process;
import com.liferay.portal.workflow.metrics.rest.client.pagination.Page;
import com.liferay.portal.workflow.metrics.rest.client.pagination.Pagination;
import com.liferay.portal.workflow.metrics.rest.resource.v1_0.test.helper.WorkflowMetricsRESTTestHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Rafael Praxedes
 */
@RunWith(Arquillian.class)
public class ProcessResourceTest extends BaseProcessResourceTestCase {

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		_documents = _workflowMetricsRESTTestHelper.getDocuments(
			testGroup.getCompanyId());

		for (Document document : _documents) {
			_workflowMetricsRESTTestHelper.deleteProcess(document);
		}
	}

	@After
	@Override
	public void tearDown() throws Exception {
		super.tearDown();

		for (Document document : _documents) {
			_workflowMetricsRESTTestHelper.restoreProcess(document);
		}

		_deleteProcesses();
	}

	@Override
	@Test
	public void testGetProcess() throws Exception {
		super.testGetProcess();

		_testGetProcess(
			true, (process1, process2) -> assertEquals(process1, process2));
		_testGetProcess(
			false, (process1, process2) -> assertEquals(process1, process2));
	}

	@Override
	@Test
	public void testGetProcessesPage() throws Exception {
		super.testGetProcessesPage();

		_deleteProcesses();

		Process process = randomProcess();

		testGetProcessesPage_addProcess(process);

		testGetProcessesPage_addProcess(randomProcess());

		Page<Process> page = processResource.getProcessesPage(
			process.getTitle(), Pagination.of(1, 2), null);

		assertEquals(
			Collections.singletonList(process), (List<Process>)page.getItems());
	}

	@Override
	@Test
	public void testGetProcessesPageWithSortInteger() throws Exception {
		testGetProcessesPageWithSort(
			EntityField.Type.INTEGER,
			(entityField, process1, process2) -> {
				process1.setInstanceCount(0L);
				process1.setOnTimeInstanceCount(0L);
				process1.setOverdueInstanceCount(0L);
				process1.setUntrackedInstanceCount(0L);

				process2.setInstanceCount(3L);
				process2.setOnTimeInstanceCount(1L);
				process2.setOverdueInstanceCount(1L);
				process2.setUntrackedInstanceCount(1L);
			});
	}

	@Override
	@Test
	public void testGetProcessTitle() throws Exception {
		Process process = randomProcess();

		testGetProcessesPage_addProcess(process);

		String title = processResource.getProcessTitle(process.getId());

		Assert.assertEquals(process.getTitle(), title);
	}

	@Ignore
	@Override
	@Test
	public void testGraphQLGetProcess() throws Exception {
	}

	@Ignore
	@Override
	@Test
	public void testGraphQLGetProcessesPage() throws Exception {
	}

	@Override
	protected String[] getAdditionalAssertFieldNames() {
		return new String[] {
			"instanceCount", "onTimeInstanceCount", "overdueInstanceCount",
			"title", "untrackedInstanceCount"
		};
	}

	@Override
	protected Process randomProcess() throws Exception {
		Process process = super.randomProcess();

		int instanceCount = RandomTestUtil.randomInt(0, 20);

		process.setInstanceCount((long)instanceCount);

		int onTimeInstanceCount = RandomTestUtil.randomInt(0, instanceCount);

		process.setOnTimeInstanceCount((long)onTimeInstanceCount);

		int overdueInstanceCount = RandomTestUtil.randomInt(
			0, instanceCount - onTimeInstanceCount);

		process.setOverdueInstanceCount((long)overdueInstanceCount);
		process.setUntrackedInstanceCount(
			(long)instanceCount - onTimeInstanceCount - overdueInstanceCount);

		return process;
	}

	@Override
	protected Process testGetProcess_addProcess() throws Exception {
		return testGetProcessesPage_addProcess(randomProcess());
	}

	@Override
	protected Process testGetProcessesPage_addProcess(Process process)
		throws Exception {

		process = _workflowMetricsRESTTestHelper.addProcess(
			testGroup.getCompanyId(), process);

		_processes.add(process);

		return process;
	}

	@Override
	protected Process testGraphQLProcess_addProcess() throws Exception {
		return testGetProcess_addProcess();
	}

	private void _deleteProcesses() throws Exception {
		for (Process process : _processes) {
			_workflowMetricsRESTTestHelper.deleteProcess(
				testGroup.getCompanyId(), process);
		}
	}

	private void _testGetProcess(
			Boolean completed,
			UnsafeBiConsumer<Process, Process, Exception> unsafeBiConsumer)
		throws Exception {

		_deleteProcesses();

		Process postProcess = randomProcess();

		postProcess.setInstanceCount(0L);
		postProcess.setOnTimeInstanceCount(0L);
		postProcess.setOverdueInstanceCount(0L);
		postProcess.setUntrackedInstanceCount(0L);

		testGetProcessesPage_addProcess(postProcess);

		_workflowMetricsRESTTestHelper.addInstance(
			testGroup.getCompanyId(), completed, postProcess.getId());

		postProcess.setInstanceCount(1L);
		postProcess.setOnTimeInstanceCount(0L);
		postProcess.setOverdueInstanceCount(0L);
		postProcess.setUntrackedInstanceCount(1L);

		Process getProcess = processResource.getProcess(
			postProcess.getId(), completed, null, null);

		unsafeBiConsumer.accept(postProcess, getProcess);
	}

	private Document[] _documents;
	private final List<Process> _processes = new ArrayList<>();

	@Inject
	private WorkflowMetricsRESTTestHelper _workflowMetricsRESTTestHelper;

}