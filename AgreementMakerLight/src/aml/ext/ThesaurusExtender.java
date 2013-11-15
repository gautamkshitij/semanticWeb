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
* A Lexicon extender that gets synonyms from a Thesaurus.                     *
*                                                                             *
* @authors Catia Pesquita, Daniel Faria                                       *
* @date 22-10-2013                                                            *
******************************************************************************/
package aml.ext;

import java.util.Set;
import java.util.Vector;

import aml.ontology.Lexicon;
import aml.ontology.Ontology;
import aml.util.StringParser;

public class ThesaurusExtender
{

//Attributes
	
	//The confidence factor of this LexiconExtender
	private final double CONFIDENCE = 0.9;
	//The type of lexical entry generated by this LexiconExtender
	private static final String TYPE = "extendedSynonym";

//Constructors
	
	/**
	 * Constructs a new ThesaurusExtender
	 */
	public ThesaurusExtender(){}

//Public Methods
	
	/**
	 * Extends the Lexicon of the given Ontology using the given Thesaurus
	 * @param o: the Ontology to extend
 	 * @param t: the Thesaurus to use for extension
	 */
	public void extendLexicon(Ontology o, Thesaurus thes)
	{
		Lexicon lex = o.getLexicon();
		Set<String> names = lex.getNames();
		//For each name in the Lexicon
		for(String n: names)
		{
			//If it is a formula, skip to the next name
			if(StringParser.isFormula(n))
				continue;
			//Otherwise, for each entry in the Thesaurus
			for(String s: thes)
			{
				//If the entry is not contained in the name, skip to next entry
				if(!n.contains(s))
					continue;
				//Otherwise, get the Thesaurus synonyms for that entry
				Vector<String> thesEntries = thes.get(s);
				//For each Thesaurus synonym, create a new synonym in the Lexicon
				Set<Integer> terms = lex.getInternalTerms(n);
				for(String t: thesEntries)
				{
					String newName = n.replace(s,t);
					for(Integer i: terms)
					{
						double weight = lex.getWeight(n,i) * CONFIDENCE;
						lex.add(i, newName, TYPE, "", weight);
					}
				}
			}
		}
	}
}