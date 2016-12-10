///*|-----------------------------------------------------------------------------
// *|            This source code is provided under the Apache 2.0 license      --
// *|  and is provided AS IS with no warranty or guarantee of fit for purpose.  --
// *|                See the project's LICENSE.md for details.                  --
// *|           Copyright Thomson Reuters 2016. All rights reserved.            --
///*|-----------------------------------------------------------------------------

package com.thomsonreuters.ema.examples.training.iprovider.series200.example260__Custom__NestedMsg;

import com.thomsonreuters.ema.access.EmaFactory;
import com.thomsonreuters.ema.access.FieldList;
import com.thomsonreuters.ema.access.GenericMsg;
import com.thomsonreuters.ema.access.Msg;
import com.thomsonreuters.ema.access.OmmException;
import com.thomsonreuters.ema.access.OmmIProviderConfig;
import com.thomsonreuters.ema.access.OmmProvider;
import com.thomsonreuters.ema.access.OmmProviderClient;
import com.thomsonreuters.ema.access.OmmProviderEvent;
import com.thomsonreuters.ema.access.OmmReal;
import com.thomsonreuters.ema.access.OmmState;
import com.thomsonreuters.ema.access.PostMsg;
import com.thomsonreuters.ema.access.RefreshMsg;
import com.thomsonreuters.ema.access.ReqMsg;
import com.thomsonreuters.ema.access.StatusMsg;
import com.thomsonreuters.ema.access.UpdateMsg;
import com.thomsonreuters.ema.rdm.EmaRdm;

class AppClient implements OmmProviderClient
{
	public long itemHandle = 0;
	public static final int APP_DOMAIN = 200;
	
	public void onReqMsg(ReqMsg reqMsg, OmmProviderEvent providerEvent)
	{
		switch(reqMsg.domainType())
		{
		case EmaRdm.MMT_LOGIN:
			processLoginRequest(reqMsg,providerEvent);
			break;
		case APP_DOMAIN:
			processCustomDomainRequest(reqMsg,providerEvent);
			break;
		default:
			processInvalidItemRequest(reqMsg,providerEvent);
			break;
		}
	}
	
	public void onGenericMsg(GenericMsg genericMsg, OmmProviderEvent providerEvent)
	{
		System.out.println("Received Generic. Item Handle: " + providerEvent.handle() + " Closure: " + providerEvent.closure() );
	}
	
	public void onRefreshMsg(RefreshMsg refreshMsg, OmmProviderEvent providerEvent) {}
	public void onStatusMsg(StatusMsg statusMsg, OmmProviderEvent providerEvent) {}
	public void onPostMsg(PostMsg postMsg, OmmProviderEvent providerEvent) {}
	public void onReissue(ReqMsg reqMsg, OmmProviderEvent providerEvent) {}
	public void onClose(ReqMsg reqMsg, OmmProviderEvent providerEvent) {}
	public void onAllMsg(Msg msg, OmmProviderEvent providerEvent) {}	
	
	void processLoginRequest(ReqMsg reqMsg, OmmProviderEvent event)
	{
		event.provider().submit(EmaFactory.createRefreshMsg().domainType(EmaRdm.MMT_LOGIN).name(reqMsg.name()).
				nameType(EmaRdm.USER_NAME).complete(true).solicited(true).
				state(OmmState.StreamState.OPEN, OmmState.DataState.OK, OmmState.StatusCode.NONE, "Login accepted"),
				event.handle());
	}
	
	void processCustomDomainRequest(ReqMsg reqMsg, OmmProviderEvent event)
	{
		if(itemHandle != 0)
		{
			processInvalidItemRequest( reqMsg, event );
			return;
		}
		
		event.provider().submit(EmaFactory.createRefreshMsg().domainType(reqMsg.domainType()).serviceId(reqMsg.serviceId()).name(reqMsg.name()).
				state(OmmState.StreamState.OPEN, OmmState.DataState.OK, OmmState.StatusCode.NONE, "Refresh Completed").solicited(true).
				privateStream(reqMsg.privateStream()).complete(true), event.handle());
		
		FieldList fieldList = EmaFactory.createFieldList();
		fieldList.add( EmaFactory.createFieldEntry().real(22, 3990, OmmReal.MagnitudeType.EXPONENT_NEG_2));
		fieldList.add( EmaFactory.createFieldEntry().real(25, 3994, OmmReal.MagnitudeType.EXPONENT_NEG_2));
		fieldList.add( EmaFactory.createFieldEntry().real(30, 9,  OmmReal.MagnitudeType.EXPONENT_0));
		fieldList.add( EmaFactory.createFieldEntry().real(31, 19, OmmReal.MagnitudeType.EXPONENT_0));
		
		event.provider().submit( EmaFactory.createGenericMsg().name("genericMsg").domainType(reqMsg.domainType()).
				payload(EmaFactory.createRefreshMsg().name(reqMsg.name()).serviceName(reqMsg.serviceName()).
				state( OmmState.StreamState.OPEN, OmmState.DataState.OK, OmmState.StatusCode.NONE, "NestedMsg").
				payload(fieldList)), event.handle());
		
		itemHandle = event.handle();
	}
	
	void processInvalidItemRequest(ReqMsg reqMsg, OmmProviderEvent event)
	{
		event.provider().submit(EmaFactory.createStatusMsg().name(reqMsg.name()).serviceName(reqMsg.serviceName()).
				domainType(reqMsg.domainType()).
				state(OmmState.StreamState.CLOSED, OmmState.DataState.SUSPECT, OmmState.StatusCode.NOT_FOUND, "Item not found"),
				event.handle());
	}
	
}

public class IProvider
{

	public static void main(String[] args)
	{
		OmmProvider provider = null;
		try
		{
			AppClient appClient = new AppClient();
			FieldList fieldList = EmaFactory.createFieldList();
			GenericMsg genericMsg = EmaFactory.createGenericMsg();
			UpdateMsg updateMsg = EmaFactory.createUpdateMsg();

			provider = EmaFactory.createOmmProvider(EmaFactory.createOmmIProviderConfig().
					operationModel(OmmIProviderConfig.OperationModel.USER_DISPATCH), appClient );
			
			while(appClient.itemHandle == 0)
			{
				provider.dispatch(1000);
				Thread.sleep(1000);
			}
			
			for(int i = 0; i < 60; i++)
			{
				
			   long startTime = System.currentTimeMillis();
					
				
			   provider.dispatch(1000);
		
			   fieldList.clear();
			   fieldList.add(EmaFactory.createFieldEntry().real(22, 3991 + i, OmmReal.MagnitudeType.EXPONENT_NEG_2));
			   fieldList.add(EmaFactory.createFieldEntry().real(30, 10 + i, OmmReal.MagnitudeType.EXPONENT_0));
			
			   provider.submit(genericMsg.clear().domainType(AppClient.APP_DOMAIN).name("genericMsg").payload(
					updateMsg.clear().payload(fieldList)), appClient.itemHandle );
				
			   while(System.currentTimeMillis() - startTime < 1000);;
			}
		}
		catch (OmmException | InterruptedException excp)
		{
			System.out.println(excp.getMessage());
		}
		finally 
		{
			if (provider != null) provider.uninitialize();
		}
	}
}
