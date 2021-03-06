package appeng.me.storage;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import appeng.api.implementations.tiles.ITileStorageMonitorable;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IExternalStorageHandler;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IStorageMonitorable;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.tile.misc.TileCondenser;

public class AEExternalHandler implements IExternalStorageHandler
{

	@Override
	public boolean canHandle(TileEntity te, ForgeDirection d, StorageChannel channel, BaseActionSource mySrc)
	{
		if ( channel == StorageChannel.ITEMS && te instanceof ITileStorageMonitorable )
			return ((ITileStorageMonitorable) te).getMonitorable( d, mySrc ) != null;

		return te instanceof TileCondenser;
	}

	@Override
	public IMEInventory getInventory(TileEntity te, ForgeDirection d, StorageChannel channel, BaseActionSource src)
	{
		if ( te instanceof TileCondenser )
		{
			if ( channel == StorageChannel.ITEMS )
				return new VoidItemInventory( (TileCondenser) te );
			else
				return new VoidFluidInventory( (TileCondenser) te );
		}

		if ( te instanceof ITileStorageMonitorable )
		{
			ITileStorageMonitorable iface = (ITileStorageMonitorable) te;
			IStorageMonitorable sm = iface.getMonitorable( d, src );

			if ( channel == StorageChannel.ITEMS && sm != null )
			{
				IMEInventory<IAEItemStack> ii = sm.getItemInventory();
				if ( ii != null )
					return ii;
			}

			if ( channel == StorageChannel.FLUIDS && sm != null )
			{
				IMEInventory<IAEFluidStack> fi = sm.getFluidInventory();
				if ( fi != null )
					return fi;
			}
		}

		return null;
	}
}
