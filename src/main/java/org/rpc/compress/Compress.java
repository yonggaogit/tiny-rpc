package org.rpc.compress;

import org.rpc.extension.SPI;

@SPI
public interface Compress {
    byte[] compress( byte[] bytes );

    byte[] decompress( byte[] bytes );
}
