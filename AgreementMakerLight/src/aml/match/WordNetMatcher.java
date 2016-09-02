/******************************************************************************
* Copyright 2013-2016 LASIGE                                                  *
*                                                                             *
* Licensed under the Apache License, Version 2.0 (the "License"); you may     *
* not use this file except in compliance with the License. You may obtain a   *
* copy of the License at http://www.apache.org/licenses/LICENSE-2.0           *
*                                                                             *
* Unless required by applicable law or agreed to in writing, software         *
* distributed under the License is distributed on an "AS IS" BASIS,           *
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.    *
* See the License for the specific language governing permissions and         *
* limitations under the License.                                              *
*                                                                             *
*******************************************************************************
* Matches Ontologies by finding literal full-name matches between their       *
* Lexicons after extension with the WordNet.                                  *
*                                                                             *
* @author Daniel Faria                                                        *
******************************************************************************/
package aml.match;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import aml.AML;
import aml.ext.LexiconExtender;
import aml.knowledge.WordNet;
import aml.ontology.Lexicon;
import aml.settings.EntityType;
import aml.settings.LexicalType;
import aml.util.StringParser;

public class WordNetMatcher implements PrimaryMatcher, LexiconExtender
{
	
//Attributes

	private static final String DESCRIPTION = "Matches entities that have one or more exact\n" +
			  								  "String matches between a Lexicon entry and a\n" +
			  								  "WordNet synonym or between WordNet synonyms";
	private static final String NAME = "WordNet Matcher";
	private static final EntityType[] SUPPORT = {EntityType.CLASS,EntityType.INDIVIDUAL,EntityType.DATA,EntityType.OBJECT};
	//The WordNet class
	private WordNet wn;
	//The type of lexical entry generated by this LexiconExtender
	private final LexicalType TYPE = LexicalType.EXTERNAL_MATCH;
	//The source of this LexiconExtender
	private final String SOURCE = "WordNet";
	//The confidence score of WordNet

	private final double CONFIDENCE = 0.9;
	
//Constructors

	/**
	 * Constructs a new WordNetMatcher
	 */
	public WordNetMatcher()
	{
		wn = new WordNet();
	}

//Public Methods

	@Override
	public void extendLexicons()
	{
		System.out.println("Extending Lexicons with WordNet Matcher");
		long time = System.currentTimeMillis()/1000;
		AML aml = AML.getInstance();
		for(EntityType e : EntityType.values())
		{
			extendLexicon(aml.getSource().getLexicon(),e,0.0);
			extendLexicon(aml.getTarget().getLexicon(),e,0.0);
		}
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
	}
	
	@Override
	public String getDescription()
	{
		return DESCRIPTION;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public EntityType[] getSupportedEntityTypes()
	{
		return SUPPORT;
	}
	
	@Override
	public Alignment match(EntityType e, double thresh) throws UnsupportedEntityTypeException
	{
		checkEntityType(e);
		System.out.println("Running WordNet Matcher");
		long time = System.currentTimeMillis()/1000;
		AML aml = AML.getInstance();
		Lexicon source = new Lexicon(aml.getSource().getLexicon());
		Lexicon target = new Lexicon(aml.getTarget().getLexicon());
		extendLexicon(source,e,thresh);
		extendLexicon(target,e,thresh);
		Alignment a = match(source,target,e,thresh);
		time = System.currentTimeMillis()/1000 - time;
		System.out.println("Finished in " + time + " seconds");
		return a;
	}
	
//Private Methods

	private void checkEntityType(EntityType e) throws UnsupportedEntityTypeException
	{
		boolean check = false;
		for(EntityType t : SUPPORT)
		{
			if(t.equals(e))
			{
				check = true;
				break;
			}
		}
		if(!check)
			throw new UnsupportedEntityTypeException(e.toString());
	}
	
	private void extendLexicon(Lexicon l, EntityType e, double thresh)
	{
		//Get the original Lexicon names into a Vector since the
		//Lexicon will be extended during the iteration (otherwise
		//we'd get a concurrentModificationException)
		Vector<String> names = new Vector<String>(l.getNames(e));
		//Iterate through the original Lexicon names
		for(String s : names)
		{
			//We don't match formulas to WordNet
			if(StringParser.isFormula(s))
				continue;
			//Find all wordForms in WordNet for each full name
			HashSet<String> wordForms = wn.getAllNounWordForms(s);
			//If there aren't any, break the name into words
			//(if it is a multi-word name) and look for wordForms
			//of each word
			if(wordForms.size() == 0 && s.contains(" "))
			{
				String[] words = s.split(" ");
				for(String w : words)
				{
					if(w.length() < 3)
						continue;
					HashSet<String> wf = wn.getAllNounWordForms(w);
					if(wf.size() == 0)
						continue;
					for(String f : wf)
						if(!f.contains(" "))
							wordForms.add(s.replace(w, f));
				}
			}
			//If there are still no wordForms, proceed to next name
			if(wordForms.size() == 0)
				continue;
			double conf = CONFIDENCE - 0.01*wordForms.size();
			if(conf < thresh)
				continue;
			Set<Integer> terms = l.getInternalEntities(e,s);
			//Add each term with the name to the extension Lexicon
			for(Integer i : terms)
			{
				double weight = conf * l.getWeight(s, i);
				if(weight < thresh)
					continue;
				for(String w : wordForms)
					l.add(i, w, "en", TYPE, SOURCE, weight);
			}
		}
	}
	
	//Matches two Lexicons
	private Alignment match(Lexicon source, Lexicon target, EntityType e, double thresh)
	{
		Alignment maps = new Alignment();
		Set<String> names = source.getNames(e);
		for(String s : names)
		{
			//Get all term indexes for the name in both ontologies
			Set<Integer> sIndexes = source.getEntities(e,s);
			Set<Integer> tIndexes = target.getEntities(e,s);
			if(tIndexes == null)
				continue;
			//Otherwise, match all indexes
			for(Integer i : sIndexes)
			{
				//Get the weight of the name for the term in the smaller lexicon
				double weight = source.getCorrectedWeight(s, i);
				Set<String> sSources = source.getSources(s, i);
				for(Integer j : tIndexes)
				{
					Set<String> tSources = target.getSources(s, j);
					//We only consider matches involving at least one WordNet synonym
					//and not envolving any external synonyms
					boolean check = (sSources.contains(SOURCE) && tSources.contains(SOURCE)) ||
							(sSources.contains(SOURCE) && tSources.contains("")) ||
							(sSources.contains("") && tSources.contains(SOURCE));
					if(!check)
						continue;

					//Get the weight of the name for the term in the larger lexicon
					double similarity = target.getCorrectedWeight(s, j);
					//Then compute the similarity, by multiplying the two weights
					similarity *= weight;
					//If the similarity is above threshold add the mapping
					if(similarity >= thresh)
						maps.add(new Mapping(i, j, similarity));
				}
			}
		}
		return maps;	
	}
}