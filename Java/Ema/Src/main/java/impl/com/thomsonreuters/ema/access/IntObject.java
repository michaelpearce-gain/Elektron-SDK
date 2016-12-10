package com.thomsonreuters.ema.access;

import com.thomsonreuters.upa.valueadd.common.VaNode;

public class IntObject  extends VaNode
{
	int _value;
	    
	    int value()
	    {
	        return _value;
	    }
	    
	    IntObject value(int value)
	    {
	        _value = value;
	        
	        return this;
	    }
	   
	    public int hashCode()
	    {
	    	//TODO: Determine if this is the correct number of times _value should be shifted
	        return (int)(_value ^ (_value >>> 32));
	    }
	    
	    public boolean equals(Object obj)
	    {
	        if (obj == this)
	        {
	            return true;
	        }
	        
	        IntObject intObj;
	        
	        try
	        {
	        	intObj = (IntObject)obj;
	        }
	        catch (ClassCastException e)
	        {
	            return false;
	        }
	        
	        return (intObj._value == _value);
	    }
	
	    IntObject clear()
	    {
	        _value = 0;
	        
	        return this;
	    }
}
