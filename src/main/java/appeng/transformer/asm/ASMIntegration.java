package appeng.transformer.asm;

import java.util.Iterator;
import java.util.List;

import net.minecraft.launchwrapper.IClassTransformer;

import org.apache.logging.log4j.Level;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import appeng.integration.IntegrationRegistry;
import appeng.integration.IntegrationType;
import appeng.transformer.annotations.integration;
import cpw.mods.fml.relauncher.FMLRelaunchLog;

public class ASMIntegration implements IClassTransformer
{

	private IntegrationRegistry integrationModules = new IntegrationRegistry();

	public ASMIntegration() {

		/**
		 * Side, Display Name, ModID ClassPostFix
		 */

		for ( IntegrationType type : IntegrationType.values() )
		{
			integrationModules.add( type );
		}
		
		// integrationModules.add( IntegrationSide.BOTH, "Thermal Expansion", "ThermalExpansion", IntegrationType.TE );
		// integrationModules.add( IntegrationSide.BOTH, "Mystcraft", "Mystcraft", IntegrationType.Mystcraft );
		// integrationModules.add( IntegrationSide.BOTH, "Greg Tech", "gregtech_addon", IntegrationType.GT );
		// integrationModules.add( IntegrationSide.BOTH, "Universal Electricity", null, IntegrationType.UE );
		// integrationModules.add( IntegrationSide.BOTH, "Logistics Pipes", "LogisticsPipes|Main", IntegrationType.LP );
		// integrationModules.add( IntegrationSide.BOTH, "Better Storage", IntegrationType.betterstorage );
		// integrationModules.add( IntegrationSide.BOTH, "Forestry", "Forestry", IntegrationType.Forestry );
		// integrationModules.add( IntegrationSide.BOTH, "Mekanism", "Mekanism", IntegrationType.Mekanism );

	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		if ( basicClass == null || transformedName.startsWith( "appeng.transformer" ) )
			return basicClass;

		if ( transformedName.startsWith( "appeng." ) )
		{
			// log( "Found " + transformedName );

			ClassNode classNode = new ClassNode();
			ClassReader classReader = new ClassReader( basicClass );
			classReader.accept( classNode, 0 );

			try
			{
				boolean reWrite = removeOptionals( classNode );

				if ( reWrite )
				{
					ClassWriter writer = new ClassWriter( ClassWriter.COMPUTE_MAXS );
					classNode.accept( writer );
					return writer.toByteArray();
				}
			}
			catch (Throwable t)
			{
				t.printStackTrace();
			}
		}
		return basicClass;
	}

	private boolean removeOptionals(ClassNode classNode)
	{
		boolean changed = false;

		if ( classNode.visibleAnnotations != null )
		{
			for (AnnotationNode an : classNode.visibleAnnotations)
			{
				if ( hasAnnotation( an, integration.Interface.class ) )
				{
					if ( stripInterface( classNode, integration.Interface.class, an ) )
						changed = true;
				}
				else if ( hasAnnotation( an, integration.InterfaceList.class ) )
				{
					for (Object o : ((List) an.values.get( 1 )))
					{
						if ( stripInterface( classNode, integration.InterfaceList.class, (AnnotationNode) o ) )
							changed = true;
					}
				}
			}
		}

		Iterator<MethodNode> i = classNode.methods.iterator();
		while (i.hasNext())
		{
			MethodNode mn = i.next();

			if ( mn.visibleAnnotations != null )
			{
				for (AnnotationNode an : mn.visibleAnnotations)
				{
					if ( hasAnnotation( an, integration.Method.class ) )
					{
						if ( stripMethod( classNode, mn, i, integration.Method.class, an ) )
							changed = true;
					}
				}

			}
		}

		if ( changed )
			log( "Updated " + classNode.name );

		return changed;
	}

	private boolean hasAnnotation(AnnotationNode ann, Class anno)
	{
		return ann.desc.equals( Type.getDescriptor( anno ) );
	}

	private boolean stripMethod(ClassNode classNode, MethodNode mn, Iterator<MethodNode> i, Class class1, AnnotationNode an)
	{
		if ( an.values.size() != 2 )
			throw new RuntimeException( "Unable to handle Method annotation on " + classNode.name );

		String iName = null;

		if ( an.values.get( 0 ).equals( "iname" ) )
			iName = (String) an.values.get( 1 );

		if ( iName != null )
		{
			IntegrationType type = IntegrationType.valueOf( iName );
			if ( !IntegrationRegistry.instance.isEnabled( type ) )
			{
				log( "Removing Method " + mn.name + " from " + classNode.name + " because " + iName + " integration is disabled." );
				i.remove();
				return true;
			}
			else
				log( "Allowing Method " + mn.name + " from " + classNode.name + " because " + iName + " integration is enabled." );
		}
		else
			throw new RuntimeException( "Unable to handle Method annotation on " + classNode.name );

		return false;
	}

	private boolean stripInterface(ClassNode classNode, Class class1, AnnotationNode an)
	{
		if ( an.values.size() != 4 )
			throw new RuntimeException( "Unable to handle Interface annotation on " + classNode.name );

		String iFace = null;
		String iName = null;

		if ( an.values.get( 0 ).equals( "iface" ) )
			iFace = (String) an.values.get( 1 );
		else if ( an.values.get( 2 ).equals( "iface" ) )
			iFace = (String) an.values.get( 3 );

		if ( an.values.get( 0 ).equals( "iname" ) )
			iName = (String) an.values.get( 1 );
		else if ( an.values.get( 2 ).equals( "iname" ) )
			iName = (String) an.values.get( 3 );

		IntegrationType type = IntegrationType.valueOf( iName );
		
		if ( iName != null && iFace != null )
		{
			if ( !IntegrationRegistry.instance.isEnabled( type ) )
			{
				log( "Removing Interface " + iFace + " from " + classNode.name + " because " + iName + " integration is disabled." );
				classNode.interfaces.remove( iFace.replace( '.', '/' ) );
				return true;
			}
			else
				log( "Allowing Interface " + iFace + " from " + classNode.name + " because " + iName + " integration is enabled." );
		}
		else
			throw new RuntimeException( "Unable to handle Method annotation on " + classNode.name );

		return false;
	}

	private void log(String string)
	{
		FMLRelaunchLog.log( "AE2-CORE", Level.INFO, string );
	}

}
