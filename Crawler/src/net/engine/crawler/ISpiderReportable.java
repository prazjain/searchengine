package net.engine.crawler;

import java.util.HashSet;

public interface ISpiderReportable {
	
	public void addInternalLinks(HashSet links);
	public void addExternalLinks(HashSet links);
	public void updateParsedLinks(String link,String words);
	public HashSet getFilteredWords();
	/**
	 *This would give the SpiderWorker an unparsed link, which it can parse.
	 */
	public String getUnparsedLink();
}
