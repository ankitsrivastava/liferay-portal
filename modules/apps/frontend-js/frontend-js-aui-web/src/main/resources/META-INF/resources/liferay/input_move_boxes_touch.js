/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

/**
 * The Input Move Boxes Touch Component.
 *
 * @deprecated since 7.2, unused
 * @module liferay-input-move-boxes-touch
 */

AUI.add(
	'liferay-input-move-boxes-touch',
	A => {
		var Lang = A.Lang;

		var STR_DOT = '.';

		var STR_SELECTED = 'selected';

		var STR_SORT_LIST_ACTIVE = 'sort-list-active';

		var SELECTOR_MOVE_OPTION = STR_DOT + 'move-option';

		var SELECTOR_SELECTED = STR_DOT + STR_SELECTED;

		var SELECTOR_SORT_LIST_ACTIVE = STR_DOT + STR_SORT_LIST_ACTIVE;

		var SELECTOR_TITLE = STR_DOT + 'title';

		var STR_CHECKED = 'checked';

		var STR_CLICK = 'click';

		var STR_NODE = 'node';

		var STR_TRUE = 'true';

		var TPL_EDIT_SELECTION =
			'<button class="btn btn-secondary edit-selection" type="button">' +
			'<i class="icon-edit"></i> ' +
			'<span class="btn-text">{0}</span>' +
			'</button>';

		var TPL_MOVE_OPTION = new A.Template(
			'<tpl for="options">',
			'<div class="handle move-option {[ values.selected ? "',
			STR_SELECTED,
			'" : "" ]}" data-value="{value}">',
			'<i class="handle icon-reorder"></i>',

			'<input {[ values.selected ? "',
			STR_CHECKED,
			'" : "" ]} class="checkbox" id="{value}CheckBox" type="checkbox" value="{value}" />',

			'<label class="title" for="{value}CheckBox" title="{name}">{name}</label>',
			'</div>',
			'</tpl>'
		);

		var TPL_SORTABLE_CONTAINER =
			'<div class="sortable-container ' +
			STR_SORT_LIST_ACTIVE +
			'"></div>';

		A.mix(
			Liferay.InputMoveBoxes.prototype,
			{
				_afterDragStart(event) {
					var dragNode = event.target.get('dragNode');

					dragNode.addClass('move-option-dragging');
				},

				_afterDropHit(event) {
					var instance = this;

					var dragNode = event.drag.get(STR_NODE);
					var dropNode = event.drop.get(STR_NODE);

					var value = dragNode.attr('data-value');

					instance._afterDropHitTask({
						dropNode,
						value,
					});
				},

				_afterDropHitFn(event) {
					var instance = this;

					instance._syncSelectedSortList();

					var dropNode = event.dropNode;
					var value = event.value;

					var moveOption = instance._sortableContainer.one(
						SELECTOR_MOVE_OPTION + '[data-value="' + value + '"]'
					);

					var selectedSortList = instance._selectedSortList;

					var dragNodeIndex = selectedSortList.indexOf(moveOption);
					var dropNodeIndex = selectedSortList.indexOf(dropNode);

					var referenceNodeIndex = dragNodeIndex + 1;

					if (dropNodeIndex > dragNodeIndex) {
						referenceNodeIndex = dragNodeIndex;
					}

					var item = instance._getOption(instance._leftBox, value);

					instance._sortLeftBox(item, referenceNodeIndex);
				},

				_getOption(box, value) {
					return box.one('option[value="' + value + '"]');
				},

				_onCheckBoxChange(event) {
					var instance = this;

					var currentTarget = event.currentTarget;

					var selected = !currentTarget.attr(STR_CHECKED);
					var value = currentTarget.val();

					var from = instance._rightBox;
					var to = instance._leftBox;

					if (selected) {
						from = instance._leftBox;
						to = instance._rightBox;
					}

					var option = instance._getOption(from, value);

					option.attr(STR_SELECTED, true);
					option.attr('data-selected', !selected);

					instance._moveItem(from, to);

					to.attr('selectedIndex', -1);

					instance._toggleMoveOption(currentTarget, option);
				},

				_onEditSelectionClick(event) {
					var instance = this;

					var btn = event.currentTarget;

					btn.toggleClass('active');

					var btnText = Liferay.Language.get('edit');

					if (btn.hasClass('active')) {
						btnText = Liferay.Language.get('stop-editing');
					}

					btn.one('.btn-text').text(btnText);

					var sortableContainer = instance._sortableContainer;

					sortableContainer.toggleClass('edit-list-active');
					sortableContainer.toggleClass(STR_SORT_LIST_ACTIVE);
				},

				_renderButtons() {
					var instance = this;

					var buttonTpl = Lang.sub(TPL_EDIT_SELECTION, [
						Liferay.Language.get('edit'),
					]);

					instance._editSelection = A.Node.create(buttonTpl);

					instance._sortableContainer.placeBefore(
						instance._editSelection
					);
				},

				_renderSortList() {
					var instance = this;

					var options = instance._contentBox.all(
						'.choice-selector option'
					);

					var sortableContainer = instance._sortableContainer;

					var data = [];

					options.each(item => {
						data.push({
							name: item.html(),
							selected: item.attr('data-selected') === STR_TRUE,
							value: item.val(),
						});
					});

					TPL_MOVE_OPTION.render(
						{
							options: data,
						},
						sortableContainer
					);

					instance._sortable = new A.Sortable({
						container: sortableContainer,
						handles: [sortableContainer.all('.handle')],
						nodes: SELECTOR_MOVE_OPTION,
						opacity: '0.2',
					});

					instance._sortable.delegate.dd
						.plug(A.Plugin.DDConstrained, {
							constrain: sortableContainer,
						})
						.plug(A.Plugin.DDWinScroll, {
							horizontal: false,
						});

					instance._syncSelectedSortList();
				},

				_sortLeftBox(item, index) {
					var instance = this;

					var leftBox = instance._leftBox;

					var referenceNode = leftBox.all('option').item(index);

					leftBox.insertBefore(item, referenceNode);
				},

				_syncSelectedSortList() {
					var instance = this;

					instance._selectedSortList = instance._sortableContainer.all(
						SELECTOR_MOVE_OPTION + SELECTOR_SELECTED
					);
				},

				_toggleMoveOption(checkbox, option) {
					var instance = this;

					var moveOption = checkbox.ancestor(SELECTOR_MOVE_OPTION);

					moveOption.toggleClass(STR_SELECTED);

					instance._syncSelectedSortList();

					if (moveOption.hasClass(STR_SELECTED)) {
						var index = instance._selectedSortList.indexOf(
							moveOption
						);

						instance._sortLeftBox(option, index);
					}
				},

				bindUI() {
					var instance = this;

					var dd = instance._sortable.delegate.dd;

					instance._editSelection.on(
						STR_CLICK,
						A.bind('_onEditSelectionClick', instance)
					);

					dd.after('drag:drophit', A.bind('_afterDropHit', instance));

					dd.after('drag:start', A.bind('_afterDragStart', instance));

					instance._contentBox.delegate(
						STR_CLICK,
						event => {
							event.preventDefault();
						},
						SELECTOR_SORT_LIST_ACTIVE + ' ' + SELECTOR_TITLE
					);

					instance._sortableContainer.delegate(
						'change',
						A.bind('_onCheckBoxChange', instance),
						'.checkbox'
					);
				},

				renderUI() {
					var instance = this;

					instance._contentBox = instance.get('contentBox');

					instance._sortableContainer = A.Node.create(
						TPL_SORTABLE_CONTAINER
					);

					instance._contentBox.append(instance._sortableContainer);

					instance._renderBoxes();
					instance._renderButtons();
					instance._renderSortList();

					instance._afterDropHitTask = A.debounce(
						'_afterDropHitFn',
						50,
						instance
					);
				},
			},
			true
		);
	},
	'',
	{
		requires: [
			'aui-base',
			'aui-template-deprecated',
			'dd-constrain',
			'dd-scroll',
			'liferay-input-move-boxes',
			'sortable',
		],
	}
);
