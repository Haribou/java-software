package com.avaya.cap;

import java.util.ArrayList;
import java.util.List;

class AnalysisPipeline
{
	private final ArrayList<String> entityIdsField = new ArrayList<>(),
					 	  	  		analysisIdsField = new ArrayList<>();
	
	List<String> getEntityIds()
	{
		return entityIdsField;
	}

	void addEntityId(Token entityIdToken)
	{
		entityIdsField.add(entityIdToken.toString());
	}

	List<String> getAnalysisIds()
	{
		return analysisIdsField;
	}

	void addAnalysisId(Token analysisIdToken)
	{
		analysisIdsField.add(analysisIdToken.toString());
	}
}
