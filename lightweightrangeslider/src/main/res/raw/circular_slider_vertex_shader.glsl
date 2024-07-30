

attribute vec4 a_Position;     		// Per-vertex position information we will pass in.
uniform mat4 u_model;
uniform mat4 u_view;
uniform mat4 u_projection;



void main()                    		// The entry point for our vertex shader.
{

                                        // It will be interpolated across the triangle.
   gl_Position = u_projection * u_view * u_model * a_Position;   // gl_Position is a special variable used to store the final position.
                                             // Multiply the vertex by the matrix to get the final point in
}                                           // normalized screen coordinates.
