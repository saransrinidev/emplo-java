import { api } from "./client";

export const chatApi = {
  send: (message: string) =>
    api.post<{ response: string }>("/chat", { message }),
};
