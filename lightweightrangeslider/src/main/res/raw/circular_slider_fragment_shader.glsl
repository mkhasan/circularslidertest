precision mediump float;       		// Set the default precision to medium. We don't need as high of a
                                        // precision in the fragment shader.

                                        // triangle per fragment.
uniform vec4  u_color;

void main()                    		// The entry point for our fragment shader.
{
   gl_FragColor = u_color;

}
