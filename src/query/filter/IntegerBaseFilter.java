package query.filter;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import implementations.dm_kernel.user.JCL_FacadeImpl;
import interfaces.kernel.JCL_facade;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import query.filter.operators.*;

public class IntegerBaseFilter {
	private JCL_facade jcl;
	private Map<String, Integer> dimensionMeta;
	
	public IntegerBaseFilter() {
    	jcl = JCL_FacadeImpl.getInstance();
		dimensionMeta = JCL_FacadeImpl.GetHashMap("Dimension");
	}
	
    @SuppressWarnings("unchecked")
	public void filtra(List<String> columns, List<Integer> operators, List<String> args, 
			List<Integer> intraOpFilter, int machineID, int coreID){
    	System.out.print("");
		System.out.println("Iniciando Filter. core " + coreID);

		Int2ObjectMap<IntList> localBase = (Int2ObjectMap<IntList>) jcl.getValue(machineID+"_core_"+coreID).getCorrectResult();
		
		Int2ObjectMap<IntList> filterResults = new Int2ObjectOpenHashMap<>();
		filterResults = runFilter(localBase, localBase, columns.get(0), operators.get(0), args.get(0));
		
		for (int i=1; i<operators.size()&&operators.get(i)!=7; i++) {
			Int2ObjectMap<IntList> aux = new Int2ObjectOpenHashMap<>();
			aux = runFilter(filterResults, localBase, columns.get(i), operators.get(i), args.get(i));
			
			filterResults.clear();
			filterResults = null;
			filterResults = new Int2ObjectOpenHashMap<>(aux);
			aux.clear();
			aux = null;
		}
		
		Object2ObjectMap<IntList, IntList> cleanResult = generateReult(filterResults, columns);
		
		localBase = null;
		jcl.deleteGlobalVar(machineID+"_core_"+coreID);
		
		jcl.instantiateGlobalVar(machineID+"_filter_core_"+coreID, cleanResult);

		System.out.println("Finalizou a Filtragem. core " + coreID + "size: " + cleanResult.size());
    }
    
    private Int2ObjectMap<IntList> runFilter(Int2ObjectMap<IntList> tuples, Int2ObjectMap<IntList> localBase,
    													 String column, int opID, String arg) {
    	Int2ObjectMap<IntList> filterResult = new Int2ObjectOpenHashMap<>();
    	FilterOperator filtOp = checkOperator(opID);

    	int colPos = dimensionMeta.get(column);
		for(Int2ObjectMap.Entry<IntList> e : tuples.int2ObjectEntrySet()){
			int tID = e.getIntKey();
			int columnVal = e.getValue().get(colPos);
			if (filterResult.containsKey(tID)) continue;

			boolean result = filtOp.op(columnVal, Integer.parseInt(arg));
			if (result) {
				filterResult.put(tID, e.getValue());
			}
		}

		return filterResult;
    }
    
    private Object2ObjectMap<IntList, IntList> generateReult(Int2ObjectMap<IntList> filterResults, List<String> columns){
    	Object2ObjectMap<IntList, IntList> finalResult = new Object2ObjectOpenHashMap<>();
		for(Int2ObjectMap.Entry<IntList> e : filterResults.int2ObjectEntrySet()){
			IntList cleanedTuple = cleanTuple(e.getValue(), columns);
			if(!finalResult.containsKey(cleanedTuple)) {
				IntList keyList = new IntArrayList();
				keyList.add(e.getIntKey());
				finalResult.put(cleanedTuple, keyList);
			}else {
				finalResult.get(cleanedTuple).add(e.getIntKey());
			}
		}
		return finalResult;
    }
    
    private IntList cleanTuple(IntList tuple, List<String> columns) {
    	IntList cleanTuple = new IntArrayList(); 
    		
    	for(String c : columns) {
    		int colPos = dimensionMeta.get(c);
    		cleanTuple.add(tuple.get(colPos));
    	}

    	return cleanTuple;
    }
    
    /*
	 * 1 = starts with
     * 2 = ends with
     * 3 = >
     * 4 = <
	 * 5 = =
	
	 * 1 = and
	 * 2 = or
	 */
    private FilterOperator checkOperator(int opID) {
    	switch (opID) {
		case 1:
			return new StartsWithOp();
		case 2:
			return new EndsWithOp();
		case 3:
			return new GreaterThanOp();
		case 4:
			return new LessThanOp();
		case 5:
			return new EqualsToOp();
		case 6:
			return new ContainsOp();
		
		default:
			return null;
		}
    }
}
