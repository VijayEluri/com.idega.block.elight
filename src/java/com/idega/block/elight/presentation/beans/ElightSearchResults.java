package com.idega.block.elight.presentation.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.idega.core.search.business.Search;
import com.idega.core.search.business.SearchPlugin;
import com.idega.core.search.business.SearchPluginManager;
import com.idega.core.search.business.SearchQuery;
import com.idega.core.search.business.SearchResult;
import com.idega.core.search.data.SimpleSearchQuery;
import com.idega.core.search.presentation.Searcher;
import com.idega.data.StringInputStream;
import com.idega.presentation.IWContext;

/**
 * 
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version 1.0
 * 
 */
public class ElightSearchResults implements Serializable {
	
	private static final long serialVersionUID = -6155590432961913762L;
	
	private String searchParameterName = Searcher.DEFAULT_SEARCH_PARAMETER_NAME;
	
	private DocumentBuilder doc_builder;
	private static final String contents_xml_part1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><content>";
	private static final String contents_xml_part2 = "</content>";
	
	
	public Collection<List<ElightSearchResult>> search(String query) {
		
		IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
		
		Collection<SearchPlugin> plugins = SearchPluginManager.getInstance().getAllSearchPluginsInitialized(iwc.getIWMainApplication());
		
		if (plugins == null || plugins.isEmpty()) {
			
//			TODO: return than nothing found
			return getMessageToTheUser("None plugins found");
		}
		
//		TODO: filter plugins to use, maybe set through js and get as parameter
			
			/*if (getSearchPluginsToUse() != null) {
				String searchClass = searchPlugin.getClass().getName();
				String className  = searchClass.substring(searchClass.lastIndexOf('.') + 1);
				String pluginNamesOrClasses = getSearchPluginsToUse();
				if ( (pluginNamesOrClasses.indexOf(className) < 0) && (pluginNamesOrClasses.indexOf(searchClass) < 0) ) {
					continue;
				}
			}*/
		
		
//		doing just simple search right? at least for now
		Map query_map = new HashMap();
		query_map.put(getSimpleSearchParameterName(), query);
		SearchQuery search_query = new SimpleSearchQuery(query_map);
		
		Map<String, List<ElightSearchResult>> typed_search_results = new HashMap<String, List<ElightSearchResult>>();
		
		DocumentBuilder doc_builder = getDocumentBuilder();
		
		if(doc_builder == null) {
//			TODO: log
			return getMessageToTheUser("Sorry, internal error occured. Please, report to administrators.");
		}
			
		for (SearchPlugin plugin : plugins) {
			
			plugin = configureSearchPlugin(plugin);
			
			//doing just simple search right? at least for now
			if(!plugin.getSupportsSimpleSearch())
				continue;

			Search search = plugin.createSearch(search_query);
			
			Collection<SearchResult> results = search.getSearchResults();
			
			if(results == null || results.isEmpty()) {
//				TODO: maybe add plugin type still and say no results or smth
				continue;
			}
			
			String search_name = plugin.getSearchName();
			
			for (SearchResult result : results) {
				
				ElightSearchResult elight_result = new ElightSearchResult();
				
				elight_result.setTitle(result.getSearchResultName());
				elight_result.setUrl(result.getSearchResultURI());
				
				try {
					
					elight_result.setContents(doc_builder.parse(new StringInputStream(
							new StringBuilder(contents_xml_part1)
							.append(result.getSearchResultAbstract())
							.append(contents_xml_part2)
							.toString()
					)));
					
				} catch (Exception e) {
					// TODO: log
					continue;
				}
				
				elight_result.setExtraInfo(result.getSearchResultExtraInformation());
				elight_result.setTypeLabel(search_name);
				
				putResultOnType(typed_search_results, result.getSearchResultType(), elight_result);
				
/*				Map extraParameters = result.getSearchResultAttributes();
				// todo group by type
				String type = result.getSearchResultType();
*/
//				Collection rowElements = plugin.getExtraRowElements(result, iwrb);
			}
		}
		
		System.out.println("returning results: "+typed_search_results.values());
		
		return typed_search_results.values();
	}
	
	private void putResultOnType(Map<String, List<ElightSearchResult>> typed_search_results, String type, ElightSearchResult elight_result) {
		
		List<ElightSearchResult> res_list = typed_search_results.get(type);
		
		if(res_list == null) {
			res_list = new ArrayList<ElightSearchResult>();
			typed_search_results.put(type, res_list);
		}
		
		res_list.add(elight_result);
	}
	
	private DocumentBuilder getDocumentBuilder() {
		
		if(doc_builder == null) {
			
			try {
				
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setNamespaceAware(false);
				factory.setValidating(false);
				doc_builder = factory.newDocumentBuilder();
				
			} catch (Exception e) {
				// TODO: log
				e.printStackTrace();
			}
		}
		return doc_builder;
	}
	
	private List<List<ElightSearchResult>> getMessageToTheUser(String message) {
		
		List<List<ElightSearchResult>> res_map = new ArrayList<List<ElightSearchResult>>();
		List<ElightSearchResult> results_error = new ArrayList<ElightSearchResult>();
		ElightSearchResult res_error = new ElightSearchResult();
		res_error.setMessage(message);
		results_error.add(res_error);
		res_map.add(results_error);
		return res_map;
	}
	
	/**
	 * Allows subclasses to cast the search plugin to its true class and manipulate it.
	 * Remember this is a global plugin so clone it if you don't want to mess with other searches.
	 * @param searchPlugin
	 */
	protected SearchPlugin configureSearchPlugin(SearchPlugin searchPlugin) {
		return searchPlugin;
	}
	
	public String getSimpleSearchParameterName() {
		return searchParameterName;
	}
}