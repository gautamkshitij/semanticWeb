1. Read Papers on Instance Matching
2. Use Protege
3. use text (maybe ) of the entities

The given is the baseline --> improve it by adding more things




My Strategy (Considering)
1. Matchers
1.1 Synonym Matching
1.2 Acronym Matcher
1.3 Expand strings (Corp. —> corporation,) (when . is at the end)
1.4 Edit Distance
1.5 Common words in their articles  (After stop words removal)
1.6 N Grams ?


List of similarities
 Affine edit distance Smith-Waterman distance Monge-Elkan distance Cosine similarity Hamming distance Generalized edit distance Jaro-Winkler distance Q-gram Soundex distance  TF/IDF

Preprocessing
1. Stemming
2. Replace with correct spelling
3. 




Combining all those Matchers
Experiments to run: 

1. use voting (with equal weights) —> taking average of all measures
2. use vector of different similarity (slide 42)

Experiments (Goal: Increase F-Measure)
1. Baseline Program
	Precision	Recall	F-measure	Found	Correct	 Reference
	89.0%   	 81.7%    85.2%   	310  	276	 338
2. Baseline program + increased the stop words list (BAD IDEA, Time taken: huge compared to (1) ) 
	Precision	Recall	F-measure	Found	Correct	 Reference
	87.6%		81.7%	84.5%		315	276	 338
3. Instead of returning Max (i am taking average) to combine similarities. (Precision up, F down)
	Precision	Recall	F-measure	Found	Correct	Reference
	89.6%		79.0%	84.0%		298	267	338
4. 