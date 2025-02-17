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

package com.liferay.journal.internal.util;

import com.liferay.asset.display.page.model.AssetDisplayPageEntry;
import com.liferay.asset.display.page.service.AssetDisplayPageEntryLocalService;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.model.JournalArticleConstants;
import com.liferay.journal.service.JournalArticleService;
import com.liferay.layout.admin.kernel.util.Sitemap;
import com.liferay.layout.admin.kernel.util.SitemapURLProvider;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.LayoutSet;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.LayoutSetLocalService;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.kernel.xml.Element;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Eduardo García
 */
@Component(immediate = true, service = SitemapURLProvider.class)
public class JournalArticleSitemapURLProvider implements SitemapURLProvider {

	@Override
	public String getClassName() {
		return JournalArticle.class.getName();
	}

	@Override
	public void visitLayout(
			Element element, String layoutUuid, LayoutSet layoutSet,
			ThemeDisplay themeDisplay)
		throws PortalException {

		List<JournalArticle> journalArticles =
			_journalArticleService.getArticlesByLayoutUuid(
				layoutSet.getGroupId(), layoutUuid);

		boolean headCheck = true;

		if ((journalArticles != null) && journalArticles.isEmpty()) {
			headCheck = false;

			journalArticles = getDisplayPageTemplateArticles(
				layoutUuid, layoutSet);
		}

		visitArticles(
			element, layoutSet, themeDisplay, journalArticles, headCheck);
	}

	@Override
	public void visitLayoutSet(
			Element element, LayoutSet layoutSet, ThemeDisplay themeDisplay)
		throws PortalException {

		List<JournalArticle> journalArticles =
			_journalArticleService.getLayoutArticles(layoutSet.getGroupId());

		visitArticles(element, layoutSet, themeDisplay, journalArticles, true);
	}

	protected List<JournalArticle> getDisplayPageTemplateArticles(
		String layoutUuid, LayoutSet layoutSet) {

		List<JournalArticle> journalArticles = new ArrayList<>();

		Layout layout = _layoutLocalService.fetchLayoutByUuidAndGroupId(
			layoutUuid, layoutSet.getGroupId(), layoutSet.isPrivateLayout());

		if (layout == null) {
			return journalArticles;
		}

		DynamicQuery assetDisplayPageEntryDynamicQuery =
			_assetDisplayPageEntryLocalService.dynamicQuery();

		long classNameId = _portal.getClassNameId(
			JournalArticle.class.getName());

		Property classNameIdProperty = PropertyFactoryUtil.forName(
			"classNameId");

		assetDisplayPageEntryDynamicQuery.add(
			classNameIdProperty.eq(classNameId));

		Property layoutPageTemplateEntryIdProperty =
			PropertyFactoryUtil.forName("layoutPageTemplateEntryId");

		assetDisplayPageEntryDynamicQuery.add(
			layoutPageTemplateEntryIdProperty.ne(Long.valueOf(0)));

		Property plidProperty = PropertyFactoryUtil.forName("plid");

		assetDisplayPageEntryDynamicQuery.add(
			plidProperty.eq(layout.getPlid()));

		assetDisplayPageEntryDynamicQuery.setProjection(
			PropertyFactoryUtil.forName("classPK"));

		List<Long> resourcePrimKeys =
			_assetDisplayPageEntryLocalService.dynamicQuery(
				assetDisplayPageEntryDynamicQuery);

		for (Long resourcePrimKey : resourcePrimKeys) {
			try {
				JournalArticle journalArticle =
					_journalArticleService.getLatestArticle(resourcePrimKey);

				if (journalArticle.isIndexable()) {
					journalArticles.add(journalArticle);
				}
			}
			catch (Exception exception) {
				if (_log.isDebugEnabled()) {
					_log.debug(exception, exception);
				}
			}
		}

		return journalArticles;
	}

	protected Layout getDisplayPageTemplateLayout(
		long groupId, long journalArticleResourcePrimKey) {

		long classNameId = _portal.getClassNameId(
			JournalArticle.class.getName());

		AssetDisplayPageEntry assetDisplayPageEntry =
			_assetDisplayPageEntryLocalService.fetchAssetDisplayPageEntry(
				groupId, classNameId, journalArticleResourcePrimKey);

		if (assetDisplayPageEntry == null) {
			return null;
		}

		long assetDisplayPageEntryPlid = assetDisplayPageEntry.getPlid();

		return _layoutLocalService.fetchLayout(assetDisplayPageEntryPlid);
	}

	protected void visitArticles(
			Element element, LayoutSet layoutSet, ThemeDisplay themeDisplay,
			List<JournalArticle> journalArticles, boolean headCheck)
		throws PortalException {

		if (journalArticles.isEmpty()) {
			return;
		}

		Set<String> processedArticleIds = new HashSet<>();

		String portalURL = _portal.getPortalURL(layoutSet, themeDisplay);

		for (JournalArticle journalArticle : journalArticles) {
			if (processedArticleIds.contains(journalArticle.getArticleId()) ||
				(journalArticle.getStatus() !=
					WorkflowConstants.STATUS_APPROVED) ||
				(headCheck && !JournalUtil.isHead(journalArticle))) {

				continue;
			}

			String journalArticleLayoutUuid = journalArticle.getLayoutUuid();

			Layout layout = null;

			if (Validator.isNotNull(journalArticleLayoutUuid)) {
				layout = _layoutLocalService.fetchLayoutByUuidAndGroupId(
					journalArticleLayoutUuid, layoutSet.getGroupId(),
					layoutSet.isPrivateLayout());
			}
			else {
				layout = getDisplayPageTemplateLayout(
					layoutSet.getGroupId(),
					journalArticle.getResourcePrimKey());
			}

			if (layout == null) {
				continue;
			}

			String groupFriendlyURL = _portal.getGroupFriendlyURL(
				_layoutSetLocalService.getLayoutSet(
					journalArticle.getGroupId(), false),
				themeDisplay);

			StringBundler sb = new StringBundler(4);

			if (!groupFriendlyURL.startsWith(portalURL)) {
				sb.append(portalURL);
			}

			sb.append(groupFriendlyURL);
			sb.append(JournalArticleConstants.CANONICAL_URL_SEPARATOR);
			sb.append(journalArticle.getUrlTitle());

			String articleURL = _portal.getCanonicalURL(
				sb.toString(), themeDisplay, layout);

			Map<Locale, String> alternateURLs = _sitemap.getAlternateURLs(
				articleURL, themeDisplay, layout);

			for (String alternateURL : alternateURLs.values()) {
				_sitemap.addURLElement(
					element, alternateURL, null,
					journalArticle.getModifiedDate(), articleURL,
					alternateURLs);
			}

			processedArticleIds.add(journalArticle.getArticleId());
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		JournalArticleSitemapURLProvider.class);

	@Reference
	private AssetDisplayPageEntryLocalService
		_assetDisplayPageEntryLocalService;

	@Reference
	private JournalArticleService _journalArticleService;

	@Reference
	private LayoutLocalService _layoutLocalService;

	@Reference
	private LayoutSetLocalService _layoutSetLocalService;

	@Reference
	private Portal _portal;

	@Reference
	private Sitemap _sitemap;

}