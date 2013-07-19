package edu.knowitall.tac2013.stanford.annotator.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import edu.knowitall.collection.immutable.Interval;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.time.TimeAnnotations.TimexAnnotation;
import edu.stanford.nlp.time.Timex;
import edu.stanford.nlp.util.CoreMap;

import edu.knowitall.tac2013.solr.query.SolrHelper;



public class StanfordAnnotatorHelperMethods {
	
	private final StanfordCoreNLP suTimePipeline;
	private final StanfordCoreNLP corefPipeline;
	private String filePath = "/homes/gws/jgilme1/docs/";
	
	public StanfordAnnotatorHelperMethods(){
		Properties suTimeProps = new Properties();
		suTimeProps.put("annotators", "tokenize, ssplit, pos, lemma, cleanxml, ner");
		suTimeProps.put("sutime.binders", "0");
		suTimeProps.put("clean.datetags","datetime|date|dateline");
		this.suTimePipeline = new StanfordCoreNLP(suTimeProps);
		
		Properties corefProps = new Properties();
	    corefProps.put("annotators", "tokenize, ssplit, pos, lemma, cleanxml, ner, dcoref");
		this.corefPipeline = new StanfordCoreNLP(corefProps);

	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException{
		StanfordAnnotatorHelperMethods sh = new StanfordAnnotatorHelperMethods();
		sh.runSuTime("testXMLDoc");
		
	}
	
	public void runSuTime(String docID) throws FileNotFoundException, IOException{
		String filePathPlusDocId = this.filePath+docID;
		FileInputStream in = new FileInputStream(new File(filePathPlusDocId));
		String fileString = IOUtils.toString(in,"UTF-8");
		in.close();
		
		Annotation document = new Annotation(fileString);
		suTimePipeline.annotate(document);
		
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    for(CoreMap sentence: sentences){
	    	for(CoreLabel token: sentence.get(TokensAnnotation.class)){
	    		String word = token.get(TextAnnotation.class);
	    		String ne = token.get(NamedEntityTagAnnotation.class);
	    		String net = token.get(NormalizedNamedEntityTagAnnotation.class);
	    		Timex tt = token.get(TimexAnnotation.class);
	    		String tts = "";
	    		if(tt != null){
	    			tts = tt.value();
	    		}
	    		System.out.println(word+ " " + ne + " " + net + " " + tts);
	    	}
	    }
	    
	    String s =document.get(NamedEntityTagAnnotation.class);
	    System.out.println(s);

	}
	
	private String normalizeTimex(Timex t){
		if(t.timexType() == "DATE"){
	      String timexString = t.value();
	      if (timexString == null) return "";
	      String formattedString = timexString;
	      if(Pattern.matches("\\w{4}", timexString)){
	    	  formattedString = timexString +"-XX-XX";
	      }
	      else if(Pattern.matches("\\w{2}-\\w{2}",timexString)){
	    	  formattedString = "XXXX-" + timexString; 
	      }
	      else if(Pattern.matches("\\w{4}-\\w{2}", timexString)){
	    	  formattedString = timexString + "-XX";
	      }
		  return formattedString;
		}
		else{
			return "";
		}
	}
	

	
	public String getNormalizedDate(Interval charInterval, String docId, String originalString) throws IOException{
		String xmlDoc = SolrHelper.getRawDoc(docId);
		if(xmlDoc.trim().isEmpty()){
			return originalString;
		}
		else{
			Annotation document = new Annotation(xmlDoc);
			suTimePipeline.annotate(document);
	
			List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		    for(CoreMap sentence: sentences){
		    	for(CoreLabel token: sentence.get(TokensAnnotation.class)){
		    		Timex tt = token.get(TimexAnnotation.class);
		    		if(charInterval.intersects(Interval.closed(token.beginPosition(), token.endPosition()))){
		    			if(tt != null && tt.value() != null){
		    				return normalizeTimex(tt);
		    			}
		    		}
		    	}
		    }
		}
	    
	    return originalString;
	}
	
	public List<String> getCorefMentions(String xmlString) {
		Annotation document = new Annotation(xmlString);
		corefPipeline.annotate(document);
		
		Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
		for( CorefMention x : graph.get(0).getMentionsInTextualOrder()){
			System.out.println(x.toString());
		}
		List<String> x = new ArrayList<String>();
		return x;
		
	}
}
