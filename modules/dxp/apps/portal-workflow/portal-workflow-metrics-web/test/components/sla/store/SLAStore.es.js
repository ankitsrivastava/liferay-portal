/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Liferay Enterprise
 * Subscription License ("License"). You may not use this file except in
 * compliance with the License. You can obtain a copy of the License by
 * contacting Liferay, Inc. See the License for the specific language governing
 * permissions and limitations under the License, including but not limited to
 * distribution rights of the Software.
 */

import {act, renderHook} from '@testing-library/react-hooks';

import {
	START_NODE_KEYS,
	STOP_NODE_KEYS,
} from '../../../../src/main/resources/META-INF/resources/js/components/sla/Constants.es';
import {useSLA} from '../../../../src/main/resources/META-INF/resources/js/components/sla/store/SLAStore.es';
import client from '../../../mock/fetch.es';

test('Should change SLA form values', () => {
	const nodeKeys = [
		{
			compositeId: '123:enter',
			executionType: 'enter',
			id: 123,
		},
		{
			compositeId: '321:enter',
			executionType: 'enter',
			id: 321,
		},
		{
			compositeId: '1234:leave',
			executionType: 'leave',
			id: 1234,
		},
	];

	const {result} = renderHook(() => useSLA(client({}), '1', '1'));

	const {changeNodesKeys, changePauseNodes, changeValue} = result.current;

	act(() => changeValue('days', 123));
	expect(result.current.sla.days).toBe(123);

	act(() =>
		changePauseNodes(nodeKeys, () => {})([
			{compositeId: '123:enter'},
			{compositeId: '456:enter'},
			{compositeId: '321:enter'},
		])
	);
	expect(
		result.current.sla.pauseNodeKeys.nodeKeys.map(({id}) => id)
	).toMatchObject([123, 321]);

	act(() =>
		changeNodesKeys(START_NODE_KEYS, nodeKeys, () => {})([
			{compositeId: '123:enter'},
			{compositeId: '456:enter'},
			{compositeId: '321:enter'},
		])
	);
	expect(
		result.current.sla.startNodeKeys.nodeKeys.map(({id}) => id)
	).toMatchObject([123, 321]);

	act(() =>
		changeNodesKeys(STOP_NODE_KEYS, nodeKeys, () => {})([
			{compositeId: '123:enter'},
			{compositeId: '456:enter'},
			{compositeId: '321:enter'},
		])
	);
	expect(
		result.current.sla.stopNodeKeys.nodeKeys.map(({id}) => id)
	).toMatchObject([123, 321]);
});

test('Should test fetch data', () => {
	const data = {
		calendarKey: '123',
		days: null,
		description: '',
		hours: '',
		name: 'test',
		pauseNodeKeys: {nodeKeys: []},
		startNodeKeys: {nodeKeys: []},
		status: undefined,
		stopNodeKeys: {nodeKeys: []},
	};

	const {result, waitForNextUpdate} = renderHook(() =>
		useSLA(client(data), '1', '1')
	);

	act(() => result.current.fetchSLA('1'));

	return waitForNextUpdate().then(() => {
		expect(result.current.sla).toMatchObject(data);
	});
});

test('Should test fetch data without some parts', () => {
	const data = {
		days: null,
		hours: '',
		name: 'test',
		processId: '',
	};

	const {result, waitForNextUpdate} = renderHook(() =>
		useSLA(client(data), '1', '1')
	);

	act(() => result.current.fetchSLA('1'));

	return waitForNextUpdate().then(() => {
		expect(result.current.sla).toMatchObject({
			calendarKey: undefined,
			days: null,
			description: '',
			hours: '',
			name: 'test',
			pauseNodeKeys: {
				nodeKeys: [],
			},
			startNodeKeys: {
				nodeKeys: [],
			},
			status: undefined,
			stopNodeKeys: {
				nodeKeys: [],
			},
		});
	});
});

test('Should test initial state', () => {
	const defaultData = {
		calendarKey: undefined,
		days: null,
		description: '',
		hours: '',
		name: undefined,
		pauseNodeKeys: {nodeKeys: []},
		startNodeKeys: {nodeKeys: []},
		status: undefined,
		stopNodeKeys: {nodeKeys: []},
	};

	const {result, waitForNextUpdate} = renderHook(() =>
		useSLA(client({}), '1', '1')
	);

	act(() => result.current.fetchSLA('1'));

	return waitForNextUpdate().then(() => {
		expect(result.current.sla).toMatchObject(defaultData);
	});
});

test('Should test reset', () => {
	const defaultData = {
		calendarKey: undefined,
		days: null,
		description: '',
		hours: '',
		name: undefined,
		pauseNodeKeys: {nodeKeys: []},
		startNodeKeys: {nodeKeys: []},
		status: undefined,
		stopNodeKeys: {nodeKeys: []},
	};

	const {result, waitForNextUpdate} = renderHook(() =>
		useSLA(client({}), '1', '1')
	);

	act(() => result.current.resetNodes());

	return waitForNextUpdate().then(() => {
		expect(result.current.sla).toMatchObject(defaultData);
	});
});

test('Should test save data', () => {
	const defaultData = {
		calendarKey: undefined,
		days: null,
		description: '',
		hours: '',
		name: 'test',
		pauseNodeKeys: {nodeKeys: []},
		startNodeKeys: {nodeKeys: []},
		status: undefined,
		stopNodeKeys: {nodeKeys: []},
	};

	const {result, waitForNextUpdate} = renderHook(() =>
		useSLA(client(defaultData), '1', '1')
	);

	act(() => result.current.saveSLA('1', null, null));

	return waitForNextUpdate().then(() => {
		expect(result.current.sla).toMatchObject(defaultData);
	});
});

test('Should test update data', () => {
	const defaultData = {
		calendarKey: undefined,
		days: null,
		description: '',
		hours: '',
		name: 'test',
		pauseNodeKeys: {nodeKeys: []},
		startNodeKeys: {nodeKeys: []},
		status: undefined,
		stopNodeKeys: {nodeKeys: []},
	};

	const {result, waitForNextUpdate} = renderHook(() =>
		useSLA(client(defaultData), '1', '1')
	);

	act(() => result.current.saveSLA('1', '1', null));

	return waitForNextUpdate().then(() => {
		expect(result.current.sla).toMatchObject(defaultData);
	});
});
