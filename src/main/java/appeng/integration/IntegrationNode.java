package appeng.integration;

import java.lang.reflect.Field;

import appeng.api.exceptions.ModNotInstalled;
import appeng.core.AEConfig;
import appeng.core.AELog;
import cpw.mods.fml.common.Loader;

public class IntegrationNode
{

	IntegrationStage state = IntegrationStage.PREINIT;
	IntegrationStage failedStage = IntegrationStage.PREINIT;
	Throwable exception = null;

	String displayName;
	String modID;

	IntegrationType shortName;
	String name = null;
	Class classValue = null;
	Object instance;
	IIntegrationModule mod = null;

	public IntegrationNode(String dspname, String _modID, IntegrationType sName, String n) {
		displayName = dspname;
		shortName = sName;
		modID = _modID;
		name = n;
	}

	@Override
	public String toString()
	{
		return shortName.name() + ":" + state.name();
	}

	void Call(IntegrationStage stage)
	{
		if ( state != IntegrationStage.FAILED )
		{
			if ( state.ordinal() > stage.ordinal() )
				return;

			try
			{
				switch (stage)
				{
				case PREINIT:

					boolean enabled = modID == null || Loader.isModLoaded( modID );

					AEConfig.instance
							.addCustomCategoryComment(
									"ModIntegration",
									"Valid Values are 'AUTO', 'ON', or 'OFF' - defaults to 'AUTO' ; Suggested that you leave this alone unless your experiencing an issue, or wish to disable the integration for a reason." );
					String Mode = AEConfig.instance.get( "ModIntegration", displayName.replace( " ", "" ), "AUTO" ).getString();

					if ( Mode.toUpperCase().equals( "ON" ) )
						enabled = true;
					if ( Mode.toUpperCase().equals( "OFF" ) )
						enabled = false;

					if ( enabled )
					{
						classValue = getClass().getClassLoader().loadClass( name );
						mod = (IIntegrationModule) classValue.getConstructor().newInstance();
						Field f = classValue.getField( "instance" );
						f.set( classValue, instance = mod );
					}
					else
						throw new ModNotInstalled( modID );

					state = IntegrationStage.INIT;

					break;
				case INIT:
					mod.Init();
					state = IntegrationStage.POSTINIT;

					break;
				case POSTINIT:
					mod.PostInit();
					state = IntegrationStage.READY;

					break;
				case FAILED:
				default:
					break;
				}
			}
			catch (Throwable t)
			{
				failedStage = stage;
				exception = t;
				state = IntegrationStage.FAILED;
			}
		}

		if ( stage == IntegrationStage.POSTINIT )
		{
			if ( state == IntegrationStage.FAILED )
			{
				AELog.info( displayName + " - Integration Disabled" );
				if ( !(exception instanceof ModNotInstalled) )
					AELog.integration( exception );
			}
			else
			{
				AELog.info( displayName + " - Integration Enable" );
			}
		}
	}

	public boolean isActive()
	{
		if ( state == IntegrationStage.PREINIT )
			Call( IntegrationStage.PREINIT );

		return state != IntegrationStage.FAILED;
	}

}
