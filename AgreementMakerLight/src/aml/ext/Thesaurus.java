/******************************************************************************
* Copyright 2013-2013 LASIGE                                                  *
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
* A thesaurus based on one or more ontologies that derives synonym concepts   *
* from defined synonym names for ontology classes.                            *
*                                                                             *
* @authors Catia Pesquita, Daniel Faria                                       *
* @date 22-10-2013                                                            *
******************************************************************************/
package aml.ext;

import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import aml.ontology.Lexicon;
import aml.ontology.Ontology;
import aml.util.StringParser;
import aml.util.Table2;

public class Thesaurus implements Iterable<String>
{
	
//Attributes
	
	//The Thesaurus
	public Table2<String,String> thesaurus;

//Constructors
	
	/**
	 * Constructs a new empty Thesaurus
	 */
	public Thesaurus()
	{
		thesaurus = new Table2<String,String>();
	}

	/**
	 * Constructs a new Thesaurus from a given Ontology
	 * @param lex
	 */
	public Thesaurus(Ontology o)
	{
		thesaurus = new Table2<String,String>();
		buildFrom(o);
	}

//Public Methods
	
	/**
	 * Adds an entry to the Thesaurus between the given pair of synonyms
	 * @param s1: the first synonym to add to the Thesaurus
	 * @param s2: the second synonym to add to the Thesaurus
	 */
	public void add(String s1, String s2)
	{
		thesaurus.add(s1, s2);
		thesaurus.add(s2, s1);
	}

	/**
	 * Builds or extends the Thesaurus from the Lexicon of the given Ontology
	 * @param o: the Ontology to use to build the Thesaurus
	 */
	public void buildFrom(Ontology o)
	{
		Lexicon lex = o.getLexicon();
		Set<Integer> terms = lex.getTerms();
		//For each term in the Lexicon
		for(Integer i : terms)
		{
			//Compute synonym terms from its names
			addSynonymTerms(lex.getNames(i));
		}
	}

	/**
	 * @param n: the name to search in the Thesaurus
	 * @return the list of synonyms for the given name
	 */
	public Vector<String> get(String n)
	{
		return thesaurus.get(n);
	}

	@Override
	public Iterator<String> iterator()
	{
		return thesaurus.keySet().iterator();
	}

//Private Methods
	
	//Adds entries to the Thesaurus based on a set of names for a concept
	private void addSynonymTerms(Set<String> names)
	{
		//Compare the set of names pairwise
		String[] namesarray = names.toArray(new String[0]);
		for(int i = 0; i < namesarray.length; i++)
			for (int j = i; j < namesarray.length; j++)
				compareSynonyms(namesarray[i], namesarray[j]);
	}

	//Extracts subconcept synonyms from two given synonyms
	private void compareSynonyms(String synonym1, String synonym2)
	{
		//Step 0. Check if either synonym is a formula, and if so, return
		if(StringParser.isFormula(synonym1) || StringParser.isFormula(synonym2))
			return;
			
		//Step 1. Setup
		//Split the synonyms
		String[] words1 = synonym1.split(" ");
		String[] words2 = synonym2.split(" ");
		//Determine which synonym has more words, to simplify the algorithm
		String[] longerWords;
		String[] shorterWords;
		if( words1.length > words2.length )
		{
			longerWords = words1;
			shorterWords = words2;
		}
		else
		{
			longerWords = words2;
			shorterWords = words1;
		}

		//Step 2a. Determine the largest overlap between the two synonyms, starting from the end
		int distanceFromEnd;
		int lSize = longerWords.length - 1;
		int sSize = shorterWords.length - 1;
		for(distanceFromEnd = 0; distanceFromEnd < shorterWords.length; distanceFromEnd++)
		{
			 //If the word is a match, continue to the next word
			if(shorterWords[sSize - distanceFromEnd].equalsIgnoreCase(longerWords[lSize - distanceFromEnd]))
				continue;
			//Otherwise finish
			else
				break; 
		}
		
		//Step 2b. Refine the word range by checking for overlap at the beginning of the synonyms
		int distanceFromBeginning;
		for(distanceFromBeginning = 0; distanceFromBeginning < (longerWords.length - distanceFromEnd);
				distanceFromBeginning++ )
		{
			if(distanceFromBeginning >= shorterWords.length)
				return;
			if(shorterWords[distanceFromBeginning].equalsIgnoreCase(longerWords[distanceFromBeginning]))
				continue;
			else
				break;
		}

		//Step 3.  Identify the subconcept synonyms that we have found.
		int startIndex = distanceFromBeginning;
		int longerEndIndex = lSize - distanceFromEnd;
		int shorterEndIndex = sSize - distanceFromEnd;
		//If there is no correspondence between words, return
		//(e.g., in "facial vii motor nucleus" vs. "facial vii nucleus", motor has no correspondence)
		if(longerEndIndex < startIndex || shorterEndIndex < startIndex)
			return;
		//Create the longer synonym string;
		String longerSynonym = new String();
		for(int i = startIndex; i <= longerEndIndex; i++)
			longerSynonym += longerWords[i] + " ";
		longerSynonym = longerSynonym.trim();
		//Create the shorter synonym string
		String shorterSynonym = new String();
		for(int i = startIndex; i <= shorterEndIndex; i++)
			shorterSynonym += shorterWords[i] + " ";
		shorterSynonym = shorterSynonym.trim();
		add(shorterSynonym, longerSynonym);
		add(longerSynonym, shorterSynonym);
	}
}