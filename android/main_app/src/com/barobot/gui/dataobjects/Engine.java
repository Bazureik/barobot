package com.barobot.gui.dataobjects;

import java.util.ArrayList;
import java.util.List;

import org.orman.mapper.Model;

import android.content.Context;

import com.barobot.gui.database.BarobotData;
import com.barobot.hardware.Arduino;
import com.barobot.hardware.devices.BarobotConnector;
import com.barobot.parser.Queue;

public class Engine {

	private static Engine instance;
	public static Engine GetInstance(Context context){
		if (instance == null){
			instance = new Engine(context);
		}
		return instance;
	}
	
	private List<Slot> slots;
	
	private Engine(Context context)
	{
		BarobotData.StartOrmanMapping(context);
	//	BarobotDataStub.SetupDatabase();
	}
	
	public List<Slot> getSlots()
	{
		if (slots == null)
		{
			slots = Model.fetchAll(Slot.class);
		}
		return slots;
	}
	
	public Slot getSlot(int position)
	{
		return BarobotData.GetSlot(position);
	}
	
	public Product getProduct(int position)
	{
		return BarobotData.GetSlot(position).product;
	}
	
	public void updateSlot(int position, Product prod)
	{
		if (prod != null)
		{
			Slot slot = BarobotData.GetSlot(position);
			slot.product = prod;
			slot.status = "OK";
			slot.currentVolume = prod.capacity;
		
			slot.update();
		}
		invalidateData();
	}
	
	public void emptySlot(int position)
	{
		Slot slot = getSlot(position);
		slot.product = null;
		slot.status = "Empty";
		slot.currentVolume = 0;
		slot.update();
		invalidateData();
	}
	
	public void invalidateData()
	{
		slots = null;
		recipes = null;
		favoriteRecipes = null;
	}
	
	public void CacheDatabase()
	{
		invalidateData();
		getSlots();
		getRecipes();
		getFavoriteRecipes();
	}
	
	public List<Product> getProducts()
	{
		return Model.fetchAll(Product.class);
	}
	
	public List<Type> getTypes() {
		return BarobotData.GetTypes();
	}


	private List<Recipe_t> recipes;
	
	public List<Recipe_t> getRecipes()
	{
		if (recipes == null)
		{
			recipes = Filter(BarobotData.GetListedRecipes()); 
		}
		return recipes;
	}

	private List<Recipe_t> favoriteRecipes;
	
	public List<Recipe_t> getFavoriteRecipes()
	{
		if (favoriteRecipes == null)
		{
			favoriteRecipes =Filter(BarobotData.GetFavoriteRecipes()); 
		}
		return favoriteRecipes;
	}
	
	private List<Recipe_t> Filter(List<Recipe_t> recipes)
	{
		List<Recipe_t> result = new ArrayList<Recipe_t>();
		
		for(Recipe_t recipe : recipes)
		{
			if (CheckRecipe(recipe))
			{
				result.add(recipe);
			}
		}
		return result;
	}
	
	public List<Recipe_t> getAllRecipes()
	{
		return BarobotData.GetRecipes();
	}
	
	public void addRecipe(Recipe_t recipe, List<Ingredient_t> ingredients)
	{
		for(Ingredient_t ing : ingredients)
		{
			ing.insert();
			recipe.ingredients.add(ing);
		}
	}
	public void removeIngredient(Ingredient_t ingredient){
		ingredient.delete();
	}
	
	public Boolean CheckRecipe(Recipe_t recipe)
	{
		List<Integer> bottleSequence = GenerateSequence(recipe.getIngredients());
		if (bottleSequence == null)
		{
			return false; // We could not find some of the ingredients
		}
		return true;
	}

	public Boolean Pour(Recipe_t recipe)
	{
		//	List<Integer> bottleSequence = GenerateSequence(ings);
		List<Ingredient_t> ings = recipe.getIngredients();
		BarobotConnector barobot = Arduino.getInstance().barobot;

		List<Slot> slots = getSlots();
		Queue q = barobot.main_queue;
		barobot.startDoingDrink(q);

		for(Ingredient_t ing : ings){
			int position = getIngredientPosition(slots, ing);
			if (position != -1){
				int count = (int) Math.round( ing.quantity/barobot.getCapacity( position -1 ) );
				if( count == 0 ){
					count = 1;
				}
		//		Log.i("Prepare", ""+position+"/"+count );
				barobot.moveToBottle(q, position-1, false );
				for (int iter = 1; iter <= count ; iter++){
					if( iter > 1){
		//				Log.i("Prepare", "addWait" );
						int repeat_time = barobot.getRepeatTime( position-1 );
						q.addWait( repeat_time  );			// wait for refill
					}
		//			Log.i("Prepare", "pour" );
					barobot.pour(q, position-1, false);
				}
			}else{// We could not find some of the ingredients
			}
		}
		barobot.moveToStart();		// na koniec
		barobot.onDrinkFinish();

		/*
		Queue q	= Arduino.getMainQ();
		q.add( new AsyncMessage( true ) {
			@Override	
			public String getName() {
				return "on drink ready";
			}
			@Override
			public Queue run(Mainboard dev, Queue queue) {
				this.name		= "onQueueFinished";
				BarobotMain.getInstance().runOnUiThread(new Runnable() {  
	                    @Override
	                    public void run() {
	                    	mListener.onQueueFinished();
                   }});
				return null;
			}
		} );*/
		return true;
	}
	
	public List<Integer> GenerateSequence(List<Ingredient_t> ingredients)
	{
		List<Integer> bottleSequence = new ArrayList<Integer>();
		List<Slot> slots = getSlots();
		BarobotConnector barobot = Arduino.getInstance().barobot;
		for(Ingredient_t ing : ingredients)
		{
			int position = getIngredientPosition(slots, ing);
			if (position != -1){
				int count = (int) Math.round( ing.quantity/barobot.getCapacity( position -1 ) );
				if( count == 0 ){
					count = 1;
				}
				for (int iter = 1; iter <= count ; iter++){
					bottleSequence.add(position);
				}
			}else{
				return null;
			}
		}
		return bottleSequence;
	}
	
	public static int getIngredientPosition(List<Slot> slots, Ingredient_t ing){
		for(Slot sl : slots)
		{
			if (sl.product != null)
			{
				if (sl.product.liquid.id == ing.liquid.id)
				{
					return sl.position;
				}
			}
		}
		
		return -1; // Indicating that ingredient was not found
	}
}
