package com.tc.poolparty;

import java.util.List;

import com.tc.poolparty.impl.PoolPartyBean;

public interface PoolPartyManager {
	/**
	 * Create tags in AEM
	 * @param tags
	 */
	public void createTags(List<PoolPartyBean> tags);
	/**
	 * Get the taxonomy from PoolParty
	 * @return
	 */
	public List<PoolPartyBean> getTags(String concepts, boolean schemeFlag, String locale);
	
	/**
	 * Crawl text and get back the tags to be created for the page
	 * @param text
	 * @return
	 */
	public List<String> crawlText(String text);


    /**
     * Crawl text and get back the tags to be created for the page
     * @param text; language - specifies which language the text is in to only match keywords in that language tree
     * @return
     */
    public List<String> crawlText(String text, String language);
}
