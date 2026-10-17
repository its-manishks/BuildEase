/** One message in the browser-held conversation (sent back as context; never stored on the server). */
export interface ChatTurn {
    role: 'user' | 'model';
    text: string;
  }
  
  export interface ChatReply {
    reply: string;
    source: 'ai' | 'assistant';
    loginRequired: boolean;
  }
  
  export interface AiStatus {
    enabled: boolean;
  }