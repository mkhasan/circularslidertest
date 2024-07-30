uniform mat4 u_MVPMatrix;      		
attribute vec4 a_Position;
attribute vec4 a_Color;			// Per-vertex color information we will pass in.

varying vec4 v_Color;   // This will be passed into the fragment shader.
varying vec3 v_Position;


void main()                    
{
    v_Color = a_Color;
    v_Position = vec3(a_Position);
	gl_Position = u_MVPMatrix * a_Position;
}