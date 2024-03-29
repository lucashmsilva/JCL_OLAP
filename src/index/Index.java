package index;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import implementations.dm_kernel.user.JCL_FacadeImpl;
import interfaces.kernel.JCL_facade;
import interfaces.kernel.JCL_result;

import java.io.IOException;

import util.FileManip;

public class Index {
	JCL_facade jcl;
	List<Entry<String,String>> devices;
	
	public Index() {
		jcl = JCL_FacadeImpl.getInstance();
		jcl.register(JCL_Index.class, "JCL_Index");
		jcl.register(JCL_IntegerBaseIndex.class, "JCL_IntegerBaseIndex");
		devices = jcl.getDevices();
	}

	// cria arquivos metadata em todas as maquinas do cluster
	public void loadMetadata(String subpath)
	{
		String mesure = null, dimension = null;
		try {
			// le asrquivos metadata
			mesure = FileManip.metaDataToString(subpath + "measuresnames.mg");
			dimension = FileManip.metaDataToString(subpath + "dimensionsnames.mg");
		} catch (IOException e) {
			e.printStackTrace();
		}

		Object [][] args = new Object[devices.size()][];
		for(int i=0;i < devices.size(); i++) {
			Object [] a = {mesure, dimension};
			args[i] = a; 
		}

		//escreve os arquivos metadata em todas as maquinas do cluster
		List<Future<JCL_result>> tickets = jcl.executeAll("JCL_IntegerBaseIndex", "writeMetaData", args);
		jcl.getAllResultBlocking(tickets);
		tickets.forEach(jcl::removeResult);

		//cria as hashMaps com os metadados para cada maquina
		tickets = jcl.executeAll("JCL_IntegerBaseIndex", "readMetaData"); 
		jcl.getAllResultBlocking(tickets);
		tickets.forEach(jcl::removeResult);
}

	public void createIndex(String origin){
		List<Future<JCL_result>> tickets = new ArrayList<Future<JCL_result>>(); 
		int j = 0;
		for(Entry<String,String> e : devices) {
			int n = jcl.getDeviceCore(e);
			for(int i=0;i<n;i++) {
				Object [] args = {new Integer(j), new Integer(i)};
				tickets.add(jcl.executeOnDevice(e,"JCL_Index", "createIndexFrom"+origin, args));
			}
			j++;
		}
		jcl.getAllResultBlocking(tickets);
		
		tickets.forEach(jcl::removeResult);

		/*System.out.println("Inverted Index");
		for(int x=0;x<4;x++) 
			System.out.println("CORE " + x + " " + jcl.getValue(0+"_invertedIndex_"+x).getCorrectResult());
		System.out.println("Mesure Index");
		for(int x=0;x<4;x++)
			System.out.println("CORE " + x + " " + jcl.getValue(0+"_mesureIndex_"+x).getCorrectResult());*/
	}
	
	public void createIntegerIndex() {
		List<Future<JCL_result>> tickets = new ArrayList<Future<JCL_result>>(); 
		int j = 0;
		for(Entry<String,String> e : devices) {
			int n = jcl.getDeviceCore(e);
			for(int i=0;i<n;i++) {
				Object [] args = {new Integer(j), new Integer(i)};
				tickets.add(jcl.executeOnDevice(e,"JCL_IntegerBaseIndex", "createIndex", args));
			}
			j++;
		}
		jcl.getAllResultBlocking(tickets);
		
		tickets.forEach(jcl::removeResult);

	}
	
	public void deleteIndices(int nDimensions) {
		for(int i=0;i<devices.size();i++) {
			int n = jcl.getDeviceCore(devices.get(i));
			for(int j=0;j<n;j++) {
				for(int k=0; k<nDimensions; k++){
					jcl.deleteGlobalVar(i+"_invertedIndex_"+k+"_"+j);
				}
				jcl.deleteGlobalVar(i+"_mesureIndex_"+j);
			}
		}
	}
}
